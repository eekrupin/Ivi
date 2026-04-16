package ru.ekrupin.ivi.data.sync

import androidx.room.withTransaction
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.SyncConflictDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.SyncPetMembershipDao
import ru.ekrupin.ivi.data.local.dao.SyncUserDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.SyncPetMembershipEntity
import ru.ekrupin.ivi.data.local.entity.SyncUserEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.data.sync.remote.RemoteBootstrapResponse
import ru.ekrupin.ivi.data.sync.remote.RemoteChangesResponse
import ru.ekrupin.ivi.data.sync.remote.RemoteSyncEventType
import ru.ekrupin.ivi.data.sync.remote.RemoteSyncPet
import ru.ekrupin.ivi.data.sync.remote.RemoteSyncPetEvent
import ru.ekrupin.ivi.data.sync.remote.RemoteSyncWeightEntry
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus

interface SyncSnapshotStore {
    suspend fun importBootstrapSnapshot(snapshot: RemoteBootstrapResponse)
    suspend fun applyIncrementalChanges(changes: RemoteChangesResponse)
}

class RoomSyncSnapshotStore @Inject constructor(
    private val database: IviDatabase,
    private val petDao: PetDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val weightEntryDao: WeightEntryDao,
    private val syncConflictDao: SyncConflictDao,
    private val syncUserDao: SyncUserDao,
    private val syncPetMembershipDao: SyncPetMembershipDao,
    private val syncOutboxDao: SyncOutboxDao,
) : SyncSnapshotStore {
    override suspend fun importBootstrapSnapshot(snapshot: RemoteBootstrapResponse) {
        check(syncOutboxDao.countAll() == 0) {
            "Bootstrap import в V1 разрешен только при пустом outbox"
        }

        val syncTime = LocalDateTime.now()
        database.withTransaction {
            syncConflictDao.deleteAll()
            syncUserDao.deleteAll()
            syncUserDao.insertAll(snapshot.snapshot.users.map { it.toEntity(syncTime) })

            syncPetMembershipDao.deleteAll()
            syncPetMembershipDao.insertAll(snapshot.snapshot.memberships.map { it.toEntity(syncTime) })

            replacePet(snapshot.snapshot.pets.firstOrNull(), syncTime)
            replaceEventTypes(snapshot.snapshot.eventTypes, syncTime)
            replacePetEvents(snapshot.snapshot.petEvents, syncTime)
            replaceWeightEntries(snapshot.snapshot.weightEntries, syncTime)
        }
    }

    override suspend fun applyIncrementalChanges(changes: RemoteChangesResponse) {
        val syncTime = LocalDateTime.now()
        database.withTransaction {
            syncUserDao.insertAll(changes.changes.users.map { it.toEntity(syncTime) })
            syncPetMembershipDao.insertAll(changes.changes.memberships.map { it.toEntity(syncTime) })

            changes.changes.pets.firstOrNull()?.let { upsertPet(it, syncTime) }
            changes.changes.eventTypes.forEach { upsertEventType(it, syncTime) }
            changes.changes.petEvents.forEach { upsertPetEvent(it, syncTime) }
            changes.changes.weightEntries.forEach { upsertWeightEntry(it, syncTime) }
            applyTombstones(changes, syncTime)
        }
    }

    private suspend fun replacePet(pet: RemoteSyncPet?, syncTime: LocalDateTime) {
        if (pet == null) {
            petDao.deleteAll()
            return
        }
        val existing = petDao.getPet()
        petDao.insert(
            PetEntity(
                id = existing?.id ?: AppConstants.PET_ID,
                name = pet.name,
                birthDate = pet.birthDate,
                photoUri = existing?.photoUri,
                createdAt = pet.createdAt,
                updatedAt = pet.serverUpdatedAt,
                remoteId = pet.remoteId,
                serverVersion = pet.serverVersion,
                serverUpdatedAt = pet.serverUpdatedAt,
                deletedAt = pet.deletedAt,
                syncState = SyncState.SYNCED,
                lastSyncedAt = syncTime,
            ),
        )
    }

    private suspend fun replaceEventTypes(items: List<RemoteSyncEventType>, syncTime: LocalDateTime) {
        eventTypeDao.deleteAll()
        items.forEach { upsertEventType(it, syncTime) }
    }

    private suspend fun replacePetEvents(items: List<RemoteSyncPetEvent>, syncTime: LocalDateTime) {
        petEventDao.deleteAll()
        items.forEach { upsertPetEvent(it, syncTime) }
    }

    private suspend fun replaceWeightEntries(items: List<RemoteSyncWeightEntry>, syncTime: LocalDateTime) {
        weightEntryDao.deleteAll()
        items.forEach { upsertWeightEntry(it, syncTime) }
    }

    private suspend fun upsertPet(pet: RemoteSyncPet, syncTime: LocalDateTime) {
        replacePet(pet, syncTime)
    }

    private suspend fun upsertEventType(item: RemoteSyncEventType, syncTime: LocalDateTime) {
        val existing = eventTypeDao.getByRemoteId(item.remoteId)
        if (existing?.syncState == SyncState.CONFLICT) return
        eventTypeDao.insert(
            EventTypeEntity(
                id = existing?.id ?: 0,
                name = item.name,
                category = EventCategory.valueOf(item.category),
                defaultDurationDays = item.defaultDurationDays,
                isActive = item.isActive,
                colorArgb = item.colorArgb,
                iconKey = item.iconKey,
                createdAt = item.createdAt,
                updatedAt = item.serverUpdatedAt,
                remoteId = item.remoteId,
                serverVersion = item.serverVersion,
                serverUpdatedAt = item.serverUpdatedAt,
                deletedAt = item.deletedAt,
                syncState = SyncState.SYNCED,
                lastSyncedAt = syncTime,
            ),
        )
    }

    private suspend fun upsertPetEvent(item: RemoteSyncPetEvent, syncTime: LocalDateTime) {
        val eventType = eventTypeDao.getByRemoteId(item.eventTypeRemoteId)
            ?: error("EventType ${item.eventTypeRemoteId} is missing for pet event import")
        val existing = petEventDao.getByRemoteId(item.remoteId)
        if (existing?.syncState == SyncState.CONFLICT) return
        petEventDao.insert(
            PetEventEntity(
                id = existing?.id ?: 0,
                petId = AppConstants.PET_ID,
                eventTypeId = eventType.id,
                eventDate = item.eventDate,
                dueDate = item.dueDate,
                comment = item.comment,
                notificationsEnabled = item.notificationsEnabled,
                status = PetEventStatus.valueOf(item.status),
                createdAt = item.createdAt,
                updatedAt = item.serverUpdatedAt,
                remoteId = item.remoteId,
                serverVersion = item.serverVersion,
                serverUpdatedAt = item.serverUpdatedAt,
                deletedAt = item.deletedAt,
                syncState = SyncState.SYNCED,
                lastSyncedAt = syncTime,
            ),
        )
    }

    private suspend fun upsertWeightEntry(item: RemoteSyncWeightEntry, syncTime: LocalDateTime) {
        val existing = weightEntryDao.getByRemoteId(item.remoteId)
        if (existing?.syncState == SyncState.CONFLICT) return
        weightEntryDao.insert(
            WeightEntryEntity(
                id = existing?.id ?: 0,
                petId = AppConstants.PET_ID,
                date = item.date,
                weightGrams = item.weightGrams,
                comment = item.comment,
                createdAt = item.createdAt,
                updatedAt = item.serverUpdatedAt,
                remoteId = item.remoteId,
                serverVersion = item.serverVersion,
                serverUpdatedAt = item.serverUpdatedAt,
                deletedAt = item.deletedAt,
                syncState = SyncState.SYNCED,
                lastSyncedAt = syncTime,
            ),
        )
    }

    private suspend fun applyTombstones(changes: RemoteChangesResponse, syncTime: LocalDateTime) {
        changes.tombstones.forEach { tombstone ->
            when (tombstone.entityType) {
                "EVENT_TYPE" -> {
                    val existing = eventTypeDao.getByRemoteId(tombstone.remoteId) ?: return@forEach
                    if (existing.syncState == SyncState.CONFLICT) return@forEach
                    eventTypeDao.insert(
                        existing.copy(
                            deletedAt = tombstone.deletedAt,
                            updatedAt = tombstone.deletedAt,
                            serverVersion = tombstone.version,
                            serverUpdatedAt = tombstone.deletedAt,
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = syncTime,
                            isActive = false,
                        ),
                    )
                }

                "PET_EVENT" -> {
                    val existing = petEventDao.getByRemoteId(tombstone.remoteId) ?: return@forEach
                    if (existing.syncState == SyncState.CONFLICT) return@forEach
                    petEventDao.insert(
                        existing.copy(
                            deletedAt = tombstone.deletedAt,
                            updatedAt = tombstone.deletedAt,
                            serverVersion = tombstone.version,
                            serverUpdatedAt = tombstone.deletedAt,
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = syncTime,
                        ),
                    )
                }

                "WEIGHT_ENTRY" -> {
                    val existing = weightEntryDao.getByRemoteId(tombstone.remoteId) ?: return@forEach
                    if (existing.syncState == SyncState.CONFLICT) return@forEach
                    weightEntryDao.insert(
                        existing.copy(
                            deletedAt = tombstone.deletedAt,
                            updatedAt = tombstone.deletedAt,
                            serverVersion = tombstone.version,
                            serverUpdatedAt = tombstone.deletedAt,
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = syncTime,
                        ),
                    )
                }

                "PET" -> {
                    val existing = petDao.getPet() ?: return@forEach
                    petDao.insert(
                        existing.copy(
                            deletedAt = tombstone.deletedAt,
                            updatedAt = tombstone.deletedAt,
                            serverVersion = tombstone.version,
                            serverUpdatedAt = tombstone.deletedAt,
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = syncTime,
                        ),
                    )
                }
            }
        }
    }

    private fun ru.ekrupin.ivi.data.sync.remote.RemoteSyncUser.toEntity(syncTime: LocalDateTime) = SyncUserEntity(
        remoteId = remoteId,
        email = email,
        displayName = displayName,
        serverVersion = serverVersion,
        serverUpdatedAt = serverUpdatedAt,
        deletedAt = deletedAt,
        createdAt = createdAt,
        lastSyncedAt = syncTime,
    )

    private fun ru.ekrupin.ivi.data.sync.remote.RemoteSyncMembership.toEntity(syncTime: LocalDateTime) = SyncPetMembershipEntity(
        remoteId = remoteId,
        petRemoteId = petRemoteId,
        userRemoteId = userRemoteId,
        role = role,
        status = status,
        serverVersion = serverVersion,
        serverUpdatedAt = serverUpdatedAt,
        deletedAt = deletedAt,
        createdAt = createdAt,
        lastSyncedAt = syncTime,
    )
}
