package ru.ekrupin.ivi.data.sync

import androidx.room.withTransaction
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.SyncConflictDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.pet.remote.PetAccessRemoteDataSource
import ru.ekrupin.ivi.data.pet.remote.RemotePetAccessPet
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

interface ReplaceLocalDataFromServerRecovery {
    suspend fun replaceLocalDataFromServer(): SyncRecoveryResult
}

interface PublishLocalDataToServerRecovery {
    suspend fun publishLocalDataToServer(): SyncRecoveryResult
}

class ReplaceLocalDataFromServerUseCase @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val syncEngine: SyncEngine,
    private val syncOutboxStore: SyncOutboxStore,
) : ReplaceLocalDataFromServerRecovery {
    override suspend fun replaceLocalDataFromServer(): SyncRecoveryResult {
        val session = authSessionManager.getSession()
        if (!session.isAuthenticated) return SyncRecoveryResult.Error("Сначала войдите в синхронизацию")

        return try {
            syncOutboxStore.deleteAll()
            syncEngine.bootstrapImport(session.baseUrl, session.accessToken)
            SyncRecoveryResult.Success
        } catch (exception: SyncHttpException) {
            exception.toRecoveryError()
        } catch (exception: Exception) {
            SyncRecoveryResult.Error(exception.message ?: "Не удалось загрузить данные с сервера")
        }
    }
}

class PublishLocalDataToServerUseCase @Inject constructor(
    private val database: IviDatabase,
    private val authSessionManager: AuthSessionManager,
    private val petAccessRemoteDataSource: PetAccessRemoteDataSource,
    private val syncEngine: SyncEngine,
    private val syncStateStore: SyncStateStore,
    private val petDao: PetDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val weightEntryDao: WeightEntryDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncConflictDao: SyncConflictDao,
    private val payloadFactory: SyncPayloadFactory,
) : PublishLocalDataToServerRecovery {
    override suspend fun publishLocalDataToServer(): SyncRecoveryResult {
        val session = authSessionManager.getSession()
        if (!session.isAuthenticated) return SyncRecoveryResult.Error("Сначала войдите в синхронизацию")

        val localPet = petDao.getPet() ?: return SyncRecoveryResult.Error("На устройстве нет локального питомца для отправки")

        return try {
            val remotePet = ensureRemotePet(session.baseUrl, session.accessToken, localPet.name, localPet.birthDate)
            val mutationCount = rebuildOutboxFromLocalData(remotePet)

            if (mutationCount == 0) {
                syncStateStore.setRequiresBootstrap(false)
                return SyncRecoveryResult.Success
            }

            syncStateStore.setRequiresBootstrap(false)
            when (val pushResult = syncEngine.drainOutbox(session.baseUrl, session.accessToken, deviceId = deviceId(), limit = 500)) {
                PushDrainResult.Empty -> SyncRecoveryResult.Success
                PushDrainResult.RequiresBootstrap -> SyncRecoveryResult.RequiresBootstrap
                is PushDrainResult.Applied -> if (pushResult.conflictCount > 0) {
                    SyncRecoveryResult.ConflictsDetected
                } else {
                    SyncRecoveryResult.Success
                }
            }
        } catch (exception: SyncHttpException) {
            exception.toRecoveryError()
        } catch (exception: Exception) {
            SyncRecoveryResult.Error(exception.message ?: "Не удалось отправить локальные данные на сервер")
        }
    }

    private suspend fun ensureRemotePet(baseUrl: String, accessToken: String, name: String, birthDate: java.time.LocalDate?): RemotePetAccessPet {
        return try {
            petAccessRemoteDataSource.getCurrentPet(baseUrl, accessToken)
        } catch (exception: SyncHttpException) {
            when (exception.code) {
                404 -> try {
                    petAccessRemoteDataSource.createPet(baseUrl, accessToken, name, birthDate)
                } catch (createException: SyncHttpException) {
                    if (createException.code == 409) {
                        petAccessRemoteDataSource.getCurrentPet(baseUrl, accessToken)
                    } else {
                        throw createException
                    }
                }
                409 -> petAccessRemoteDataSource.getCurrentPet(baseUrl, accessToken)
                else -> throw exception
            }
        }
    }

    private suspend fun rebuildOutboxFromLocalData(remotePet: RemotePetAccessPet): Int {
        val now = LocalDateTime.now()
        return database.withTransaction {
            val existingPet = petDao.getPet() ?: error("Pet disappeared during local publish")
            val syncedPet = existingPet.copy(
                name = existingPet.name.ifBlank { remotePet.name },
                remoteId = remotePet.id,
                serverVersion = remotePet.version,
                serverUpdatedAt = remotePet.updatedAt,
                syncState = SyncState.SYNCED,
                lastSyncedAt = now,
            )
            petDao.insert(syncedPet)

            syncConflictDao.deleteAll()
            syncOutboxDao.deleteAll()

            val eventTypes = eventTypeDao.getActiveForSync().map { entity ->
                entity.copy(
                    remoteId = entity.remoteId.normalizedRemoteId(),
                    syncState = SyncState.PENDING_UPLOAD,
                    serverVersion = null,
                    lastSyncedAt = null,
                )
            }
            eventTypes.forEach { entity ->
                eventTypeDao.insert(entity)
                syncOutboxDao.insert(payloadFactory.eventTypeUpsert(entity, syncedPet, now, baseVersion = null))
            }

            val eventTypeById = eventTypes.associateBy { it.id }
            val petEvents = petEventDao.getActiveForSync().mapNotNull { entity ->
                val eventType = eventTypeById[entity.eventTypeId] ?: return@mapNotNull null
                entity.copy(
                    remoteId = entity.remoteId.normalizedRemoteId(),
                    syncState = SyncState.PENDING_UPLOAD,
                    serverVersion = null,
                    lastSyncedAt = null,
                ) to eventType
            }
            petEvents.forEach { (entity, eventType) ->
                petEventDao.insert(entity)
                syncOutboxDao.insert(payloadFactory.petEventUpsert(entity, syncedPet, eventType, now, baseVersion = null))
            }

            val weightEntries = weightEntryDao.getActiveForSync().map { entity ->
                entity.copy(
                    remoteId = entity.remoteId.normalizedRemoteId(),
                    syncState = SyncState.PENDING_UPLOAD,
                    serverVersion = null,
                    lastSyncedAt = null,
                )
            }
            weightEntries.forEach { entity ->
                weightEntryDao.insert(entity)
                syncOutboxDao.insert(payloadFactory.weightEntryUpsert(entity, syncedPet, now, baseVersion = null))
            }

            eventTypes.size + petEvents.size + weightEntries.size
        }
    }

    private fun deviceId(): String = "android-recovery-${UUID.randomUUID()}"

    private fun String?.normalizedRemoteId(): String = if (isValidUuid()) this!! else UUID.randomUUID().toString()

    private fun String?.isValidUuid(): Boolean = !isNullOrBlank() && runCatching { UUID.fromString(this) }
        .getOrNull()
        ?.toString()
        ?.equals(this, ignoreCase = true) == true
}

sealed interface SyncRecoveryResult {
    data object Success : SyncRecoveryResult
    data object ConflictsDetected : SyncRecoveryResult
    data object RequiresBootstrap : SyncRecoveryResult
    data class Error(val message: String) : SyncRecoveryResult
}

private fun SyncHttpException.toRecoveryError(): SyncRecoveryResult.Error = when (code) {
    401 -> SyncRecoveryResult.Error("Сессия истекла. Войдите в синхронизацию заново")
    in 500..599 -> SyncRecoveryResult.Error("Ошибка сервера: HTTP $code")
    else -> SyncRecoveryResult.Error(message ?: "HTTP $code")
}
