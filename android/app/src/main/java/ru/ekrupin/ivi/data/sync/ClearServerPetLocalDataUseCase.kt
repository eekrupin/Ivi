package ru.ekrupin.ivi.data.sync

import androidx.room.withTransaction
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.SyncConflictDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.SyncPetMembershipDao
import ru.ekrupin.ivi.data.local.dao.SyncUserDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase

class ClearServerPetLocalDataUseCase @Inject constructor(
    private val database: IviDatabase,
    private val petDao: PetDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val weightEntryDao: WeightEntryDao,
    private val syncUserDao: SyncUserDao,
    private val syncPetMembershipDao: SyncPetMembershipDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncConflictDao: SyncConflictDao,
    private val syncStateStore: SyncStateStore,
) {
    suspend operator fun invoke() {
        database.withTransaction {
            syncConflictDao.deleteAll()
            syncOutboxDao.deleteAll()
            syncPetMembershipDao.deleteAll()
            syncUserDao.deleteAll()
            weightEntryDao.deleteAll()
            petEventDao.deleteAll()
            eventTypeDao.deleteAll()
            petDao.deleteAll()
            syncStateStore.clear()
        }
    }
}
