package ru.ekrupin.ivi.data.pet.remote

data class DownloadedPetPhoto(
    val bytes: ByteArray,
    val contentType: String,
)

interface PetPhotoRemoteDataSource {
    suspend fun uploadPhoto(baseUrl: String, accessToken: String, petId: String, localPhotoUri: String): String
    suspend fun downloadPhoto(baseUrl: String, accessToken: String, petId: String, revision: String): DownloadedPetPhoto
    suspend fun deletePhoto(baseUrl: String, accessToken: String, petId: String)
}
