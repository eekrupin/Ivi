package ru.ekrupin.ivi.core.util

import android.content.Context
import android.net.Uri
import java.io.File

private const val PET_PHOTO_DIR = "pet-photos"

fun Context.copyPickedPetPhoto(sourceUri: Uri, previousPhotoUri: String?): String {
    val photoDirectory = File(filesDir, PET_PHOTO_DIR).apply { mkdirs() }
    val photoFile = File(photoDirectory, "pet_${System.currentTimeMillis()}.jpg")

    contentResolver.openInputStream(sourceUri)?.use { inputStream ->
        photoFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: error("Не удалось открыть выбранное фото")

    deleteManagedPetPhoto(previousPhotoUri)

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
