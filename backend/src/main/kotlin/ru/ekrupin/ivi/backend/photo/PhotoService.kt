package ru.ekrupin.ivi.backend.photo

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.PetPhotoRecord
import ru.ekrupin.ivi.backend.db.repository.PetPhotoRepository
import ru.ekrupin.ivi.backend.pet.PetAccessService
import java.util.UUID

class PhotoService(
    private val petAccessService: PetAccessService,
    private val petPhotoRepository: PetPhotoRepository,
) {
    fun uploadPhoto(userId: UUID, petId: UUID, contentType: String, data: ByteArray): PhotoUploadResponse {
        petAccessService.requireActiveAccess(petId, userId)
        validatePhoto(contentType, data)

        val revision = UUID.randomUUID().toString()
        val result = petPhotoRepository.upsert(petId, revision, contentType, data)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        return PhotoUploadResponse(
            petId = petId.toString(),
            photoRevision = result.revision,
            status = if (result.replaced) "REPLACED" else "STORED",
        )
    }

    fun downloadPhoto(userId: UUID, petId: UUID, revision: String?): PetPhotoRecord {
        petAccessService.requireActiveAccess(petId, userId)
        val photo = petPhotoRepository.find(petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "photo_not_found", "Фото питомца не найдено")

        if (revision != null && photo.revision != revision) {
            throw ApiException(HttpStatusCode.NotFound, "photo_not_found", "Фото питомца не найдено")
        }
        return photo
    }

    fun deletePhoto(userId: UUID, petId: UUID): PhotoDeleteResponse {
        petAccessService.requireActiveAccess(petId, userId)
        petPhotoRepository.delete(petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        return PhotoDeleteResponse(
            petId = petId.toString(),
            photoRevision = null,
            deleted = true,
        )
    }

    private fun validatePhoto(contentType: String, data: ByteArray) {
        if (data.isEmpty()) {
            throw ApiException(HttpStatusCode.BadRequest, "empty_photo", "Фото не должно быть пустым")
        }
        if (data.size > MAX_PHOTO_BYTES) {
            throw ApiException(HttpStatusCode.BadRequest, "photo_too_large", "Фото не должно превышать 5 МБ")
        }
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_photo_content_type", "Поддерживаются только JPEG и PNG")
        }
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png")
    }
}

@Serializable
data class PhotoUploadResponse(
    val petId: String,
    val photoRevision: String,
    val status: String,
)

@Serializable
data class PhotoDeleteResponse(
    val petId: String,
    val photoRevision: String?,
    val deleted: Boolean,
)
