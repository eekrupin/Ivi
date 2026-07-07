package ru.ekrupin.ivi.data.sync

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import ru.ekrupin.ivi.core.util.deleteManagedPetPhoto
import ru.ekrupin.ivi.core.util.findDownloadedPetPhotoUri
import ru.ekrupin.ivi.core.util.isDownloadedPetPhotoUri
import ru.ekrupin.ivi.core.util.saveDownloadedPetPhoto
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.pet.remote.PetPhotoRemoteDataSource
import ru.ekrupin.ivi.data.sync.model.SyncState

interface PetPhotoSnapshotSyncer {
    suspend fun syncAfterPetSnapshot(baseUrl: String, accessToken: String)
}

class PetPhotoSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: IviDatabase,
    private val petDao: PetDao,
    private val petPhotoRemoteDataSource: PetPhotoRemoteDataSource,
) : PetPhotoSnapshotSyncer {
    override suspend fun syncAfterPetSnapshot(baseUrl: String, accessToken: String) {
        val pet = petDao.getPet() ?: return
        val petRemoteId = pet.remoteId?.takeIf { it.isNotBlank() } ?: return
        val revision = pet.photoRevision

        if (revision == null) {
            if (context.isDownloadedPetPhotoUri(pet.photoUri)) {
                database.withTransaction {
                    val current = petDao.getPet() ?: return@withTransaction
                    if (current.photoRevision == null && context.isDownloadedPetPhotoUri(current.photoUri)) {
                        context.deleteManagedPetPhoto(current.photoUri)
                        petDao.insert(current.copy(photoUri = null))
                    }
                }
            }
            return
        }

        val existingDownloadedUri = context.findDownloadedPetPhotoUri(revision)
        if (existingDownloadedUri != null) {
            if (pet.photoUri != existingDownloadedUri && canReplaceWithDownloadedPhoto(pet)) {
                database.withTransaction {
                    val current = petDao.getPet() ?: return@withTransaction
                    if (current.photoRevision == revision && canReplaceWithDownloadedPhoto(current)) {
                        if (current.photoUri != existingDownloadedUri) context.deleteManagedPetPhoto(current.photoUri)
                        petDao.insert(current.copy(photoUri = existingDownloadedUri))
                    }
                }
            }
            return
        }

        val downloaded = petPhotoRemoteDataSource.downloadPhoto(baseUrl, accessToken, petRemoteId, revision)
        val downloadedUri = context.saveDownloadedPetPhoto(
            bytes = downloaded.bytes,
            contentType = downloaded.contentType,
            revision = revision,
            previousPhotoUri = pet.photoUri.takeIf { context.isDownloadedPetPhotoUri(it) },
        )
        database.withTransaction {
            val current = petDao.getPet() ?: return@withTransaction
            if (current.photoRevision == revision && canReplaceWithDownloadedPhoto(current)) {
                if (current.photoUri != downloadedUri) context.deleteManagedPetPhoto(current.photoUri)
                petDao.insert(current.copy(photoUri = downloadedUri))
            }
        }
    }

    private fun canReplaceWithDownloadedPhoto(pet: PetEntity): Boolean = shouldReplaceWithDownloadedPetPhoto(
        photoUri = pet.photoUri,
        syncState = pet.syncState,
        isDownloadedPetPhotoUri = context::isDownloadedPetPhotoUri,
    )
}

internal fun shouldReplaceWithDownloadedPetPhoto(
    photoUri: String?,
    syncState: SyncState,
    isDownloadedPetPhotoUri: (String?) -> Boolean,
): Boolean = photoUri == null || isDownloadedPetPhotoUri(photoUri) || syncState == SyncState.SYNCED
