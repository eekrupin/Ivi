package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.mapper.toEntity
import ru.ekrupin.ivi.data.sync.SyncOutboxRecorder
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.EventType
import ru.ekrupin.ivi.domain.repository.EventTypeRepository

class LocalEventTypeRepository @Inject constructor(
    private val database: IviDatabase,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val syncOutboxRecorder: SyncOutboxRecorder,
) : EventTypeRepository {
    override fun observeTypes(): Flow<List<EventType>> = eventTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveTypes(): Flow<List<EventType>> = eventTypeDao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun saveType(eventType: EventType) {
        val now = LocalDateTime.now()
        val existing = eventType.id.takeIf { it != 0L }?.let { eventTypeDao.getById(it) }
        val entity = eventType.copy(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        ).toEntity().copy(
            remoteId = existing?.remoteId ?: UUID.randomUUID().toString(),
            serverVersion = existing?.serverVersion,
            serverUpdatedAt = existing?.serverUpdatedAt,
            deletedAt = null,
            syncState = SyncState.PENDING_UPLOAD,
            lastSyncedAt = existing?.lastSyncedAt,
        )

        database.withTransaction {
            val saved = if (existing == null) {
                val id = eventTypeDao.insert(entity)
                entity.copy(id = id)
            } else {
                eventTypeDao.update(entity)
                entity
            }
            syncOutboxRecorder.enqueueEventTypeUpsert(saved)
        }
    }

    override suspend fun deleteType(id: Long) {
        val existing = eventTypeDao.getById(id) ?: return
        val now = LocalDateTime.now()

        database.withTransaction {
            if (petEventDao.countByEventTypeId(id) == 0) {
                val deleted = existing.copy(
                    isActive = false,
                    updatedAt = now,
                    deletedAt = now,
                    syncState = SyncState.PENDING_UPLOAD,
                )
                eventTypeDao.update(deleted)
                syncOutboxRecorder.enqueueEventTypeDelete(deleted)
            } else {
                val updated = existing.copy(
                    isActive = false,
                    updatedAt = now,
                    syncState = SyncState.PENDING_UPLOAD,
                )
                eventTypeDao.update(updated)
                syncOutboxRecorder.enqueueEventTypeUpsert(updated)
            }
        }
    }
}
