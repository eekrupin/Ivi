package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus

interface SyncOutboxStore {
    suspend fun pending(limit: Int): List<SyncOutboxEntity>
    suspend fun markInFlight(ids: List<Long>)
    suspend fun markPending(ids: List<Long>)
    suspend fun delete(ids: List<Long>)
}

class RoomSyncOutboxStore @Inject constructor(
    private val syncOutboxDao: SyncOutboxDao,
) : SyncOutboxStore {
    override suspend fun pending(limit: Int): List<SyncOutboxEntity> = syncOutboxDao.getByStatus(SyncOutboxStatus.PENDING, limit)

    override suspend fun markInFlight(ids: List<Long>) {
        if (ids.isEmpty()) return
        syncOutboxDao.updateStatus(ids, SyncOutboxStatus.IN_FLIGHT, LocalDateTime.now())
    }

    override suspend fun markPending(ids: List<Long>) {
        if (ids.isEmpty()) return
        syncOutboxDao.updateStatus(ids, SyncOutboxStatus.PENDING, LocalDateTime.now())
    }

    override suspend fun delete(ids: List<Long>) {
        if (ids.isEmpty()) return
        syncOutboxDao.deleteByIds(ids)
    }
}
