package ru.ekrupin.ivi.data.repository

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.core.util.saveLocalPetPhotoAsDownloadedRevision
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.pet.remote.PetPhotoRemoteDataSource
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.Pet
import ru.ekrupin.ivi.domain.repository.PetRepository

class LocalPetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: IviDatabase,
    private val petDao: PetDao,
    private val authSessionManager: AuthSessionManager,
    private val petPhotoRemoteDataSource: PetPhotoRemoteDataSource,
) : PetRepository {
    override fun observePet(): Flow<Pet?> = petDao.observePet().map { it?.toDomain() }

    override suspend fun savePet(name: String, birthDate: LocalDate?, photoUri: String?) {
        val current = petDao.observePet()
        val now = LocalDateTime.now()
        val existing = current.first()
        val pet = PetEntity(
            id = existing?.id ?: 1L,
            name = name,
            birthDate = birthDate,
            photoUri = photoUri?.takeIf { it.isNotBlank() },
            photoRevision = existing?.photoRevision,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            remoteId = existing?.remoteId ?: UUID.randomUUID().toString(),
            serverVersion = existing?.serverVersion,
            serverUpdatedAt = existing?.serverUpdatedAt,
            deletedAt = null,
            syncState = existing?.syncState ?: SyncState.SYNCED,
            lastSyncedAt = existing?.lastSyncedAt,
        )
        database.withTransaction {
            petDao.insert(pet)
        }
        syncSavedPhoto(existing = existing, saved = pet)
    }

    private suspend fun syncSavedPhoto(existing: PetEntity?, saved: PetEntity) {
        val session = authSessionManager.getSession()
        val petRemoteId = saved.remoteId?.takeIf { it.isNotBlank() }
        if (!session.isAuthenticated || petRemoteId == null) return

        val oldPhotoUri = existing?.photoUri
        val newPhotoUri = saved.photoUri
        if (newPhotoUri != null && newPhotoUri != oldPhotoUri) {
            runCatching {
                petPhotoRemoteDataSource.uploadPhoto(session.baseUrl, session.accessToken, petRemoteId, newPhotoUri)
            }.onSuccess { photoRevision ->
                database.withTransaction {
                    val current = petDao.getPet() ?: return@withTransaction
                    if (current.id == saved.id && current.photoUri == newPhotoUri) {
                        val downloadedUri = context.saveLocalPetPhotoAsDownloadedRevision(newPhotoUri, photoRevision)
                        petDao.insert(current.copy(photoUri = downloadedUri, photoRevision = photoRevision))
                    }
                }
            }
            return
        }

        if (newPhotoUri == null && oldPhotoUri != null && existing.photoRevision != null) {
            runCatching {
                petPhotoRemoteDataSource.deletePhoto(session.baseUrl, session.accessToken, petRemoteId)
            }.onSuccess {
                database.withTransaction {
                    val current = petDao.getPet() ?: return@withTransaction
                    if (current.id == saved.id && current.photoUri == null) {
                        petDao.insert(current.copy(photoRevision = null))
                    }
                }
            }
        }
    }
}
