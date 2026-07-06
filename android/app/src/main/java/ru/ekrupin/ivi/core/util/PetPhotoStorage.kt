package ru.ekrupin.ivi.core.util

import android.content.Context
import android.net.Uri
import java.io.File

private const val PET_PHOTO_DIR = "pet-photos"
private const val REMOTE_PHOTO_PREFIX = "remote_"

fun Context.copyPickedPetPhoto(sourceUri: Uri): String {
    val photoDirectory = File(filesDir, PET_PHOTO_DIR).apply { mkdirs() }
    val photoFile = File(photoDirectory, "pet_${System.currentTimeMillis()}.jpg")

    contentResolver.openInputStream(sourceUri)?.use { inputStream ->
        photoFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: error("Не удалось открыть выбранное фото")

    return Uri.fromFile(photoFile).toString()
}

fun Context.deleteManagedPetPhoto(photoUri: String?) {
    val parsedUri = photoUri?.let(Uri::parse) ?: return
    if (parsedUri.scheme != "file") return

    val photoDirectory = File(filesDir, PET_PHOTO_DIR)
    val photoFile = parsedUri.path?.let(::File) ?: return
    if (photoFile.parentFile?.canonicalPath == photoDirectory.canonicalPath) {
        photoFile.delete()
    }
}

fun Context.isDownloadedPetPhotoUri(photoUri: String?): Boolean {
    val photoFile = managedPetPhotoFile(photoUri) ?: return false
    return photoFile.name.startsWith(REMOTE_PHOTO_PREFIX)
}

fun Context.findDownloadedPetPhotoUri(revision: String): String? {
    val photoDirectory = File(filesDir, PET_PHOTO_DIR)
    return downloadedPetPhotoCandidates(photoDirectory, revision)
        .firstOrNull { it.exists() }
        ?.let(Uri::fromFile)
        ?.toString()
}

fun Context.saveDownloadedPetPhoto(
    bytes: ByteArray,
    contentType: String,
    revision: String,
    previousPhotoUri: String?,
): String {
    val photoDirectory = File(filesDir, PET_PHOTO_DIR).apply { mkdirs() }
    val photoFile = File(photoDirectory, "$REMOTE_PHOTO_PREFIX${revision.safeFilePart()}${contentType.fileExtension()}")
    photoFile.outputStream().use { it.write(bytes) }

    val newUri = Uri.fromFile(photoFile).toString()
    if (previousPhotoUri != newUri) deleteManagedPetPhoto(previousPhotoUri)
    return newUri
}

fun Context.saveLocalPetPhotoAsDownloadedRevision(photoUri: String, revision: String): String {
    val sourceUri = Uri.parse(photoUri)
    val photoDirectory = File(filesDir, PET_PHOTO_DIR).apply { mkdirs() }
    val photoFile = File(photoDirectory, "$REMOTE_PHOTO_PREFIX${revision.safeFilePart()}${photoUri.fileUriExtension()}")

    val sourceFile = managedPetPhotoFile(photoUri)
    if (sourceFile == photoFile) return photoUri
    if (sourceFile != null && sourceFile != photoFile && sourceFile.renameTo(photoFile)) {
        return Uri.fromFile(photoFile).toString()
    }

    contentResolver.openInputStream(sourceUri)?.use { inputStream ->
        photoFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: error("Не удалось открыть выбранное фото")

    val newUri = Uri.fromFile(photoFile).toString()
    if (photoUri != newUri) deleteManagedPetPhoto(photoUri)
    return newUri
}

private fun Context.managedPetPhotoFile(photoUri: String?): File? {
    val parsedUri = photoUri?.let(Uri::parse) ?: return null
    if (parsedUri.scheme != "file") return null

    val photoDirectory = File(filesDir, PET_PHOTO_DIR)
    val photoFile = parsedUri.path?.let(::File) ?: return null
    return if (photoFile.parentFile?.canonicalPath == photoDirectory.canonicalPath) photoFile else null
}

private fun downloadedPetPhotoCandidates(photoDirectory: File, revision: String): List<File> {
    val baseName = "$REMOTE_PHOTO_PREFIX${revision.safeFilePart()}"
    return listOf(File(photoDirectory, "$baseName.jpg"), File(photoDirectory, "$baseName.png"))
}

private fun String.fileExtension(): String = if (lowercase().substringBefore(';').trim() == "image/png") ".png" else ".jpg"

private fun String.fileUriExtension(): String {
    val path = Uri.parse(this).path.orEmpty().lowercase()
    return if (path.endsWith(".png")) ".png" else ".jpg"
}

private fun String.safeFilePart(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")
