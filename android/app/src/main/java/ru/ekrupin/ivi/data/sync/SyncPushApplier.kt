package ru.ekrupin.ivi.data.sync

import androidx.room.withTransaction
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.SyncConflictDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.SyncConflictEntity
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.data.sync.remote.RemoteAcceptedMutation
import ru.ekrupin.ivi.data.sync.remote.RemoteConflict

interface SyncPushApplier {
    suspend fun applyAccepted(
        outboxItems: List<SyncOutboxEntity>,
        accepted: List<RemoteAcceptedMutation>,
        syncedAt: LocalDateTime,
    )

    suspend fun applyConflicts(
        outboxItems: List<SyncOutboxEntity>,
        conflicts: List<RemoteConflict>,
        conflictedAt: LocalDateTime,
    )

    suspend fun markFailed(outboxItems: List<SyncOutboxEntity>)
}

class RoomSyncPushApplier @Inject constructor(
    private val database: IviDatabase,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncConflictDao: SyncConflictDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val weightEntryDao: WeightEntryDao,
) : SyncPushApplier {
    override suspend fun applyAccepted(
        outboxItems: List<SyncOutboxEntity>,
        accepted: List<RemoteAcceptedMutation>,
        syncedAt: LocalDateTime,
    ) {
        val byClientMutationId = outboxItems.associateBy { it.clientMutationId }
        val toDelete = mutableListOf<Long>()

        database.withTransaction {
            accepted.forEach { item ->
                val outbox = item.clientMutationId?.let(byClientMutationId::get) ?: return@forEach
                syncConflictDao.deleteByClientMutationId(outbox.clientMutationId)
                when (outbox.entityType) {
                    SyncEntityType.EVENT_TYPE -> {
                        val entity = eventTypeDao.getById(outbox.entityLocalId) ?: return@forEach
                        eventTypeDao.update(
                            entity.copy(
                                serverVersion = item.version,
                                serverUpdatedAt = syncedAt,
                                syncState = SyncState.SYNCED,
                                lastSyncedAt = syncedAt,
                            ),
                        )
                    }

                    SyncEntityType.PET_EVENT -> {
                        val entity = petEventDao.getById(outbox.entityLocalId) ?: return@forEach
                        petEventDao.insert(
                            entity.copy(
                                serverVersion = item.version,
                                serverUpdatedAt = syncedAt,
                                syncState = SyncState.SYNCED,
                                lastSyncedAt = syncedAt,
                            ),
                        )
                    }

                    SyncEntityType.WEIGHT_ENTRY -> {
                        val entity = weightEntryDao.getById(outbox.entityLocalId) ?: return@forEach
                        weightEntryDao.insert(
                            entity.copy(
                                serverVersion = item.version,
                                serverUpdatedAt = syncedAt,
                                syncState = SyncState.SYNCED,
                                lastSyncedAt = syncedAt,
                            ),
                        )
                    }
                }
                toDelete += outbox.id
            }

            if (toDelete.isNotEmpty()) {
                syncOutboxDao.deleteByIds(toDelete)
            }
        }
    }

    override suspend fun applyConflicts(
        outboxItems: List<SyncOutboxEntity>,
        conflicts: List<RemoteConflict>,
        conflictedAt: LocalDateTime,
    ) {
        val byClientMutationId = outboxItems.associateBy { it.clientMutationId }
        val conflictIds = mutableListOf<Long>()

        database.withTransaction {
            conflicts.forEach { item ->
                val outbox = item.clientMutationId?.let(byClientMutationId::get) ?: return@forEach
                syncConflictDao.upsert(
                    SyncConflictEntity(
                        entityType = outbox.entityType,
                        entityLocalId = outbox.entityLocalId,
                        entityRemoteId = outbox.entityRemoteId,
                        clientMutationId = outbox.clientMutationId,
                        reason = item.reason,
                        serverVersion = item.serverVersion,
                        serverRecordJson = item.serverRecordJson,
                        conflictedAt = conflictedAt,
                    ),
                )
                when (outbox.entityType) {
                    SyncEntityType.EVENT_TYPE -> {
                        val entity = eventTypeDao.getById(outbox.entityLocalId) ?: return@forEach
                        eventTypeDao.update(entity.copy(syncState = SyncState.CONFLICT, lastSyncedAt = conflictedAt))
                    }

                    SyncEntityType.PET_EVENT -> {
                        val entity = petEventDao.getById(outbox.entityLocalId) ?: return@forEach
                        petEventDao.insert(entity.copy(syncState = SyncState.CONFLICT, lastSyncedAt = conflictedAt))
                    }

                    SyncEntityType.WEIGHT_ENTRY -> {
                        val entity = weightEntryDao.getById(outbox.entityLocalId) ?: return@forEach
                        weightEntryDao.insert(entity.copy(syncState = SyncState.CONFLICT, lastSyncedAt = conflictedAt))
                    }
                }
                conflictIds += outbox.id
            }

            if (conflictIds.isNotEmpty()) {
                syncOutboxDao.updateStatus(conflictIds, SyncOutboxStatus.FAILED, conflictedAt)
            }
        }
    }

    override suspend fun markFailed(outboxItems: List<SyncOutboxEntity>) {
        val ids = outboxItems.map { it.id }
        if (ids.isEmpty()) return
        syncOutboxDao.updateStatus(ids, SyncOutboxStatus.FAILED, LocalDateTime.now())
    }
}
