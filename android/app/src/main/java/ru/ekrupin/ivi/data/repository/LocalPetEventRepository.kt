package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.mapper.toEntity
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.data.sync.SyncOutboxRecorder
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.PetEvent
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.PetEventRepository

class LocalPetEventRepository @Inject constructor(
    private val database: IviDatabase,
    private val petEventDao: PetEventDao,
    private val reminderScheduler: ReminderScheduler,
    private val syncOutboxRecorder: SyncOutboxRecorder,
) : PetEventRepository {
    override fun observeEvents(): Flow<List<PetEvent>> = petEventDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeEvent(id: Long): Flow<PetEvent?> = petEventDao.observeById(id).map { it?.toDomain() }

    override suspend fun saveEvent(event: PetEvent): Long {
        val now = LocalDateTime.now()
        val existing = event.id.takeIf { it != 0L }?.let { petEventDao.getById(it) }
        val entity = event.copy(
            createdAt = existing?.createdAt ?: if (event.id == 0L) now else event.createdAt,
            updatedAt = now,
        ).toEntity().copy(
            remoteId = existing?.remoteId ?: UUID.randomUUID().toString(),
            serverVersion = existing?.serverVersion,
            serverUpdatedAt = existing?.serverUpdatedAt,
            deletedAt = null,
            syncState = SyncState.PENDING_UPLOAD,
            lastSyncedAt = existing?.lastSyncedAt,
        )

        val saved = database.withTransaction {
            if (existing == null) {
                val newId = petEventDao.insert(entity)
                val persisted = entity.copy(id = newId)
                syncOutboxRecorder.enqueuePetEventUpsert(persisted)
                persisted
            } else {
                petEventDao.insert(entity)
                syncOutboxRecorder.enqueuePetEventUpsert(entity)
                entity
            }
        }
        reminderScheduler.refreshAll()
        return saved.id
    }

    override suspend fun updateStatus(id: Long, status: PetEventStatus) {
        val existing = petEventDao.getById(id) ?: return
        saveEvent(existing.toDomain().copy(status = status))
        reminderScheduler.refreshAll()
    }

    override suspend fun deleteEvent(id: Long) {
        val existing = petEventDao.getById(id) ?: return
        val now = LocalDateTime.now()
        database.withTransaction {
            val deleted = existing.copy(
                deletedAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING_UPLOAD,
            )
            petEventDao.insert(deleted)
            syncOutboxRecorder.enqueuePetEventDelete(deleted)
        }
        reminderScheduler.refreshAll()
    }
}
