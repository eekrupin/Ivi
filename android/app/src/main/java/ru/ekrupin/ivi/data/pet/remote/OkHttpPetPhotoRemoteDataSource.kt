package ru.ekrupin.ivi.data.pet.remote

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

class OkHttpPetPhotoRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) : PetPhotoRemoteDataSource {
    override suspend fun uploadPhoto(baseUrl: String, accessToken: String, petId: String, localPhotoUri: String): String =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(localPhotoUri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Не удалось открыть фото питомца")
            val contentType = uri.contentType()
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/pets/$petId/photo")
                .header("Authorization", "Bearer $accessToken")
                .put(bytes.toRequestBody(contentType.toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw SyncHttpException(response.code, "Pet photo upload failed: HTTP ${response.code} $body")
                }
                JSONObject(body).getString("photoRevision")
            }
        }

    override suspend fun downloadPhoto(
        baseUrl: String,
        accessToken: String,
        petId: String,
        revision: String,
    ): DownloadedPetPhoto = withContext(Dispatchers.IO) {
        val encodedRevision = URLEncoder.encode(revision, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/pets/$petId/photo?revision=$encodedRevision")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body
            val contentType = responseBody?.contentType()?.toString()?.takeIf { it.isNotBlank() } ?: JPEG_CONTENT_TYPE
            val bytes = responseBody?.bytes() ?: ByteArray(0)
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Pet photo download failed: HTTP ${response.code}")
            }
            DownloadedPetPhoto(
                bytes = bytes,
                contentType = contentType,
            )
        }
    }

    override suspend fun deletePhoto(baseUrl: String, accessToken: String, petId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/pets/$petId/photo")
            .header("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Pet photo delete failed: HTTP ${response.code} $body")
            }
        }
    }

    private fun Uri.contentType(): String {
        val resolverType = context.contentResolver.getType(this)
        if (resolverType == PNG_CONTENT_TYPE || resolverType == JPEG_CONTENT_TYPE) return resolverType
        val path = path.orEmpty().lowercase()
        return if (path.endsWith(".png")) PNG_CONTENT_TYPE else JPEG_CONTENT_TYPE
    }

    private companion object {
        const val JPEG_CONTENT_TYPE = "image/jpeg"
        const val PNG_CONTENT_TYPE = "image/png"
    }
}
