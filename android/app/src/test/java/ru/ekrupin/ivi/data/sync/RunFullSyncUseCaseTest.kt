package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException
import java.time.LocalDateTime

class RunFullSyncUseCaseTest {
    @Test
    fun firstBootstrap_runsWhenCursorMissingAndOutboxEmpty() = runBlocking {
        val engine = FakeSyncEngine()
        val stateStore = FakeSyncStateStore(cursor = null)
        val photoSyncer = FakePetPhotoSnapshotSyncer()
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(), photoSyncer)

        val result = useCase("http://localhost:8080", "token")

        assertEquals(1, engine.bootstrapCalls)
        assertEquals(0, engine.pushCalls)
        assertEquals(0, engine.changesCalls)
        assertEquals(1, photoSyncer.calls)
        assertTrue(result is SyncRunResult.Success)
    }

    @Test
    fun ordinaryChangesPull_runsWhenCursorExistsAndOutboxEmpty() = runBlocking {
        val engine = FakeSyncEngine()
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val photoSyncer = FakePetPhotoSnapshotSyncer()
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(), photoSyncer)

        val result = useCase("http://localhost:8080", "token")

        assertEquals(0, engine.bootstrapCalls)
        assertEquals(0, engine.pushCalls)
        assertEquals(1, engine.changesCalls)
        assertEquals(1, photoSyncer.calls)
        assertTrue(result is SyncRunResult.Success)
    }

    @Test
    fun pushThenChanges_runsWhenOutboxHasPendingItems() = runBlocking {
        val engine = FakeSyncEngine().apply {
            pushResult = PushDrainResult.Applied(acceptedCount = 1, conflictCount = 0, cursor = "changes:2000")
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:1000")
        val outboxItem = fakeOutboxItem()
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(outboxItem)), FakePetPhotoSnapshotSyncer())

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
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(fakeOutboxItem())), FakePetPhotoSnapshotSyncer())

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
        val useCase = RunFullSyncUseCase(engine, stateStore, FakeSyncOutboxStore(mutableListOf(fakeOutboxItem())), FakePetPhotoSnapshotSyncer())

        val result = useCase("http://localhost:8080", "token")

        assertEquals(1, engine.pushCalls)
        assertEquals(1, engine.changesCalls)
        assertEquals(SyncRunResult.ConflictsDetected, result)
    }

    @Test
    fun noServerPet_isReturnedAndPhotoSyncDoesNotRun() = runBlocking {
        val engine = FakeSyncEngine().apply {
            changesError = SyncHttpException(404, "current_pet_not_found")
        }
        val photoSyncer = FakePetPhotoSnapshotSyncer()
        val useCase = RunFullSyncUseCase(
            engine,
            FakeSyncStateStore(cursor = "changes:1000"),
            FakeSyncOutboxStore(),
            photoSyncer,
        )

        val result = useCase("http://localhost:8080", "token")

        assertEquals(SyncRunResult.NoServerPet, result)
        assertEquals(1, engine.changesCalls)
        assertEquals(0, photoSyncer.calls)
    }

    @Test
    fun photoSnapshotFailure_doesNotBreakSuccessfulDomainSync() = runBlocking {
        val photoSyncer = FakePetPhotoSnapshotSyncer(throwOnSync = true)
        val useCase = RunFullSyncUseCase(
            FakeSyncEngine(),
            FakeSyncStateStore(cursor = "changes:1000"),
            FakeSyncOutboxStore(),
            photoSyncer,
        )

        val result = useCase("http://localhost:8080", "token")

        assertEquals(1, photoSyncer.calls)
        assertEquals(
            SyncRunResult.Success(bootstrapPerformed = false, pushPerformed = false, changesPerformed = true),
            result,
        )
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
    var changesError: Exception? = null

    override suspend fun bootstrapImport(baseUrl: String, accessToken: String) {
        bootstrapCalls += 1
    }

    override suspend fun pullChanges(baseUrl: String, accessToken: String) {
        changesCalls += 1
        changesError?.let { throw it }
    }

    override suspend fun drainOutbox(baseUrl: String, accessToken: String, deviceId: String, limit: Int): PushDrainResult {
        pushCalls += 1
        return pushResult
    }
}

private class FakePetPhotoSnapshotSyncer(
    private val throwOnSync: Boolean = false,
) : PetPhotoSnapshotSyncer {
    var calls = 0

    override suspend fun syncAfterPetSnapshot(baseUrl: String, accessToken: String) {
        calls += 1
        if (throwOnSync) error("photo sync failed")
    }
}
