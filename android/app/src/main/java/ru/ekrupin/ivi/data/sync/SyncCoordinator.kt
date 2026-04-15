package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.sync.remote.SyncRemoteDataSource

class SyncCoordinator @Inject constructor(
    private val syncRemoteDataSource: SyncRemoteDataSource,
    private val syncSnapshotStore: SyncSnapshotStore,
    private val syncStateStore: SyncStateStore,
    private val syncOutboxDao: SyncOutboxDao,
) {
    suspend fun bootstrapImport(baseUrl: String, accessToken: String) {
        require(syncOutboxDao.countAll() == 0) {
            "Bootstrap import в V1 разрешен только при пустом outbox"
        }
        val response = syncRemoteDataSource.bootstrap(baseUrl, accessToken)
        syncSnapshotStore.importBootstrapSnapshot(response)
        syncStateStore.saveBootstrapCursor(response.cursor, LocalDateTime.now())
    }

    suspend fun pullChanges(baseUrl: String, accessToken: String) {
        val state = syncStateStore.get()
        val cursor = requireNotNull(state.cursor) {
            "Cursor отсутствует. Сначала выполните bootstrap import"
        }
        val response = syncRemoteDataSource.changes(baseUrl, accessToken, cursor)
        syncSnapshotStore.applyIncrementalChanges(response)
        syncStateStore.saveChangesCursor(response.cursor, LocalDateTime.now())
    }
}
