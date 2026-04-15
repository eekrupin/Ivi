package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import java.time.LocalDateTime

class RunFullSyncUseCaseTest {
    @Test
    fun firstBootstrap_runsWhenCursorMissingAndOutboxEmpty() = runBlocking {
        val engine = FakeSyncEngine()
        val stateStore = FakeSyncStateStore(cursor = null)
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore())

        val result = useCase("http://localhost:8080", "token")

        assertEquals(1, engine.bootstrapCalls)
        assertEquals(0, engine.pushCalls)
        assertEquals(0, engine.changesCalls)
        assertTrue(result is SyncRunResult.Success)
    }

    @Test
    fun ordinaryChangesPull_runsWhenCursorExistsAndOutboxEmpty() = runBlocking {
        val engine = FakeSyncEngine()
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore())

        val result = useCase("http://localhost:8080", "token")

        assertEquals(0, engine.bootstrapCalls)
        assertEquals(0, engine.pushCalls)
        assertEquals(1, engine.changesCalls)
        assertTrue(result is SyncRunResult.Success)
    }

    @Test
    fun pushThenChanges_runsWhenOutboxHasPendingItems() = runBlocking {
        val engine = FakeSyncEngine().apply {
            pushResult = PushDrainResult.Applied(acceptedCount = 1, conflictCount = 0, cursor = "changes:2000")
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val outboxItem = fakeOutboxItem()
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(outboxItem)))

        val result = useCase("http://localhost:8080", "token")

        assertEquals(0, engine.bootstrapCalls)
        assertEquals(1, engine.pushCalls)
        assertEquals(1, engine.changesCalls)
        assertTrue(result is SyncRunResult.Success)
    }

    @Test
    fun requiresBootstrap_isReturnedWhenServerRequestsItDuringPush() = runBlocking {
        val engine = FakeSyncEngine().apply {
            pushResult = PushDrainResult.RequiresBootstrap
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(fakeOutboxItem())))

        val result = useCase("http://localhost:8080", "token")

        assertTrue(result is SyncRunResult.RequiresBootstrap)
        assertEquals(0, engine.changesCalls)
    }

    @Test
    fun conflictsAreReportedButChangesStillPulled() = runBlocking {
        val engine = FakeSyncEngine().apply {
            pushResult = PushDrainResult.Applied(acceptedCount = 0, conflictCount = 1, cursor = "changes:3000")
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(fakeOutboxItem())))

        val result = useCase("http://localhost:8080", "token")

        assertEquals(1, engine.pushCalls)
        assertEquals(1, engine.changesCalls)
        assertEquals(SyncRunResult.ConflictsDetected, result)
    }

    private fun fakeOutboxItem() = SyncOutboxEntity(
        id = 1,
        entityType = SyncEntityType.EVENT_TYPE,
        entityLocalId = 1,
        entityRemoteId = "remote-1",
        operation = SyncOperation.UPSERT,
        payloadJson = "{}",
        baseVersion = 1,
        clientMutationId = "m1",
        status = SyncOutboxStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}

private class FakeSyncEngine : SyncEngine {
    var bootstrapCalls = 0
    var changesCalls = 0
    var pushCalls = 0
    var pushResult: PushDrainResult = PushDrainResult.Empty

    override suspend fun bootstrapImport(baseUrl: String, accessToken: String) {
        bootstrapCalls += 1
    }

    override suspend fun pullChanges(baseUrl: String, accessToken: String) {
        changesCalls += 1
    }

    override suspend fun drainOutbox(baseUrl: String, accessToken: String, deviceId: String, limit: Int): PushDrainResult {
        pushCalls += 1
        return pushResult
    }
}
