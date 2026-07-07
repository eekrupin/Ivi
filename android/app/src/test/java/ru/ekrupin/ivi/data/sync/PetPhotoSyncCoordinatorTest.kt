package ru.ekrupin.ivi.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ekrupin.ivi.data.sync.model.SyncState

class PetPhotoSyncCoordinatorTest {
    @Test
    fun shouldReplaceWithDownloadedPetPhoto_allowsServerSyncedLocalUri() {
        val result = shouldReplaceWithDownloadedPetPhoto(
            photoUri = "file:///data/user/0/ru.ekrupin.ivi/files/pet-photos/pet_123.jpg",
            syncState = SyncState.SYNCED,
            isDownloadedPetPhotoUri = { false },
        )

        assertTrue(result)
    }

    @Test
    fun shouldReplaceWithDownloadedPetPhoto_keepsNonSyncedLocalUri() {
        val result = shouldReplaceWithDownloadedPetPhoto(
            photoUri = "file:///data/user/0/ru.ekrupin.ivi/files/pet-photos/pet_123.jpg",
            syncState = SyncState.PENDING_UPLOAD,
            isDownloadedPetPhotoUri = { false },
        )

        assertFalse(result)
    }

    @Test
    fun shouldReplaceWithDownloadedPetPhoto_allowsExistingDownloadedUri() {
        val result = shouldReplaceWithDownloadedPetPhoto(
            photoUri = "file:///data/user/0/ru.ekrupin.ivi/files/pet-photos/remote_revision.jpg",
            syncState = SyncState.PENDING_UPLOAD,
            isDownloadedPetPhotoUri = { true },
        )

        assertTrue(result)
    }
}
