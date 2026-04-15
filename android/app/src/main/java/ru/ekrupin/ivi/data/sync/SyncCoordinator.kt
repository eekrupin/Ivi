package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.sync.remote.SyncRemoteDataSource
import ru.ekrupin.ivi.data.sync.remote.RemotePushMutation
import ru.ekrupin.ivi.data.sync.remote.RemotePushRequest

class SyncCoordinator @Inject constructor(
    private val syncRemoteDataSource: SyncRemoteDataSource,
    private val syncSnapshotStore: SyncSnapshotStore,
    private val syncStateStore: SyncStateStore,
    private val syncOutboxStore: SyncOutboxStore,
    private val syncPushApplier: SyncPushApplier,
) : SyncEngine {
    override suspend fun bootstrapImport(baseUrl: String, accessToken: String) {
        require(syncOutboxStore.pending(limit = 1).isEmpty()) {
            "Bootstrap import в V1 разрешен только при пустом outbox"
        }
        val response = syncRemoteDataSource.bootstrap(baseUrl, accessToken)
        syncSnapshotStore.importBootstrapSnapshot(response)
        syncStateStore.saveBootstrapCursor(response.cursor, LocalDateTime.now())
    }

    override suspend fun pullChanges(baseUrl: String, accessToken: String) {
        val state = syncStateStore.get()
        val cursor = requireNotNull(state.cursor) {
            "Cursor отсутствует. Сначала выполните bootstrap import"
        }
        val response = syncRemoteDataSource.changes(baseUrl, accessToken, cursor)
        syncSnapshotStore.applyIncrementalChanges(response)
        syncStateStore.saveChangesCursor(response.cursor, LocalDateTime.now())
    }

    override suspend fun drainOutbox(baseUrl: String, accessToken: String, deviceId: String, limit: Int): PushDrainResult {
        val pending = syncOutboxStore.pending(limit)
        if (pending.isEmpty()) return PushDrainResult.Empty

        val ids = pending.map { it.id }
        syncOutboxStore.markInFlight(ids)
        return try {
            val state = syncStateStore.get()
            val response = syncRemoteDataSource.push(
                baseUrl = baseUrl,
                accessToken = accessToken,
                request = RemotePushRequest(
                    deviceId = deviceId,
                    lastKnownCursor = state.cursor,
                    mutations = pending.map { item ->
                        RemotePushMutation(
                            clientMutationId = item.clientMutationId,
                            entityId = item.entityRemoteId,
                            baseVersion = item.baseVersion,
                            entityType = item.entityType.name,
                            operation = item.operation.name,
                            payloadJson = item.payloadJson,
                        )
                    },
                ),
            )

            if (response.requiresBootstrap) {
                syncPushApplier.markFailed(pending)
                syncStateStore.setRequiresBootstrap(true)
                PushDrainResult.RequiresBootstrap
            } else {
                val now = LocalDateTime.now()
                syncPushApplier.applyAccepted(pending, response.accepted, now)
                syncPushApplier.applyConflicts(pending, response.conflicts, now)
                syncStateStore.saveChangesCursor(response.cursor, now)
                syncStateStore.setRequiresBootstrap(false)

                val unresolvedIds = pending.filterNot { outbox ->
                    response.accepted.any { it.clientMutationId == outbox.clientMutationId } ||
                        response.conflicts.any { it.clientMutationId == outbox.clientMutationId }
                }.map { it.id }
                syncOutboxStore.markPending(unresolvedIds)

                PushDrainResult.Applied(
                    acceptedCount = response.accepted.size,
                    conflictCount = response.conflicts.size,
                    cursor = response.cursor,
                )
            }
        } catch (error: Exception) {
            syncOutboxStore.markPending(ids)
            throw error
        }
    }
}

sealed interface PushDrainResult {
    data object Empty : PushDrainResult
    data object RequiresBootstrap : PushDrainResult
    data class Applied(
        val acceptedCount: Int,
        val conflictCount: Int,
        val cursor: String,
    ) : PushDrainResult
}
