package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.SyncStateDao
import ru.ekrupin.ivi.data.local.entity.SyncStateEntity

data class SyncReadState(
    val cursor: String?,
    val lastBootstrapAt: LocalDateTime?,
    val lastChangesAt: LocalDateTime?,
    val lastSuccessfulReadAt: LocalDateTime?,
    val requiresBootstrap: Boolean,
)

interface SyncStateStore {
    suspend fun get(): SyncReadState
    suspend fun saveBootstrapCursor(cursor: String, timestamp: LocalDateTime)
    suspend fun saveChangesCursor(cursor: String, timestamp: LocalDateTime)
    suspend fun setRequiresBootstrap(value: Boolean)
}

class RoomSyncStateStore @Inject constructor(
    private val syncStateDao: SyncStateDao,
) : SyncStateStore {
    override suspend fun get(): SyncReadState {
        val state = syncStateDao.get()
        return SyncReadState(
            cursor = state?.cursor,
            lastBootstrapAt = state?.lastBootstrapAt,
            lastChangesAt = state?.lastChangesAt,
            lastSuccessfulReadAt = state?.lastSuccessfulReadAt,
            requiresBootstrap = state?.requiresBootstrap ?: false,
        )
    }

    override suspend fun saveBootstrapCursor(cursor: String, timestamp: LocalDateTime) {
        val existing = syncStateDao.get()
        syncStateDao.insert(
            SyncStateEntity(
                id = 1,
                cursor = cursor,
                lastBootstrapAt = timestamp,
                lastChangesAt = existing?.lastChangesAt,
                lastSuccessfulReadAt = timestamp,
                requiresBootstrap = false,
            ),
        )
    }

    override suspend fun saveChangesCursor(cursor: String, timestamp: LocalDateTime) {
        val existing = syncStateDao.get()
        syncStateDao.insert(
            SyncStateEntity(
                id = 1,
                cursor = cursor,
                lastBootstrapAt = existing?.lastBootstrapAt,
                lastChangesAt = timestamp,
                lastSuccessfulReadAt = timestamp,
                requiresBootstrap = false,
            ),
        )
    }

    override suspend fun setRequiresBootstrap(value: Boolean) {
        val existing = syncStateDao.get()
        syncStateDao.insert(
            SyncStateEntity(
                id = 1,
                cursor = existing?.cursor,
                lastBootstrapAt = existing?.lastBootstrapAt,
                lastChangesAt = existing?.lastChangesAt,
                lastSuccessfulReadAt = existing?.lastSuccessfulReadAt,
                requiresBootstrap = value,
            ),
        )
    }
}
