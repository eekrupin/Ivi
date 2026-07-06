package ru.ekrupin.ivi.backend.photo

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.ekrupin.ivi.backend.BackendPostgresTest
import ru.ekrupin.ivi.backend.db.repository.PetPhotoRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository

class PetPhotoRepositoryTest : BackendPostgresTest() {
    private val users by lazy { UserRepository(databaseFactory) }
    private val pets by lazy { PetRepository(databaseFactory) }
    private val photos by lazy { PetPhotoRepository(databaseFactory) }

    @Test
    fun `upsert stores photo revision and bumps pet version`() {
        val owner = users.create("owner@example.test", "hash", "owner")
        val pet = createPet(owner.id)

        val result = photos.upsert(pet.id, "rev-1", "image/png", byteArrayOf(1, 2, 3))

        assertNotNull(result)
        assertFalse(result.replaced)
        assertEquals("rev-1", pets.findById(pet.id)!!.photoRevision)
        assertEquals(pet.version + 1, pets.findById(pet.id)!!.version)
        assertContentEquals(byteArrayOf(1, 2, 3), photos.find(pet.id)!!.data)
    }

    @Test
    fun `replacement keeps one photo and bumps revision`() {
        val owner = users.create("owner@example.test", "hash", "owner")
        val pet = createPet(owner.id)
        photos.upsert(pet.id, "rev-1", "image/png", byteArrayOf(1))

        val result = photos.upsert(pet.id, "rev-2", "image/jpeg", byteArrayOf(2, 3))

        assertNotNull(result)
        assertTrue(result.replaced)
        val stored = photos.find(pet.id)!!
        assertEquals("rev-2", stored.revision)
        assertEquals("image/jpeg", stored.contentType)
        assertContentEquals(byteArrayOf(2, 3), stored.data)
        assertEquals("rev-2", pets.findById(pet.id)!!.photoRevision)
    }

    @Test
    fun `delete clears photo revision and bumps pet version`() {
        val owner = users.create("owner@example.test", "hash", "owner")
        val pet = createPet(owner.id)
        photos.upsert(pet.id, "rev-1", "image/png", byteArrayOf(1))
        val withPhoto = pets.findById(pet.id)!!

        val deleted = photos.delete(pet.id)

        assertEquals(true, deleted)
        assertNull(photos.find(pet.id))
        val afterDelete = pets.findById(pet.id)!!
        assertNull(afterDelete.photoRevision)
        assertEquals(withPhoto.version + 1, afterDelete.version)
    }

    private fun createPet(ownerId: java.util.UUID) =
        assertIs<PetRepository.CreatePetWithOwnerResult.Created>(
            pets.createWithOwnerMembership("Иви", null, ownerId),
        ).pet
}
