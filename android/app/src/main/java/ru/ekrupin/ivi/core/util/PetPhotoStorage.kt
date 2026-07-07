package ru.ekrupin.ivi.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

private const val PET_PHOTO_DIR = "pet-photos"
private const val REMOTE_PHOTO_PREFIX = "remote_"

fun Context.copyPickedPetPhoto(sourceUri: Uri): String {
    val photoDirectory = File(filesDir, PET_PHOTO_DIR).apply { mkdirs() }
    val photoFile = File(photoDirectory, "pet_${System.currentTimeMillis()}.jpg")

    val bytes = contentResolver.openInputStream(sourceUri)?.use { inputStream ->
        normalizePetPhotoBytes(inputStream.readBytes(), JPEG_CONTENT_TYPE)
    } ?: error("Не удалось открыть выбранное фото")
    writePetPhotoAtomically(photoFile) { it.write(bytes) }

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
    val normalizedBytes = normalizePetPhotoBytes(bytes, contentType)
    writePetPhotoAtomically(photoFile) { it.write(normalizedBytes) }

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
    if (sourceFile != null && sourceFile != photoFile && sourceFile.moveToPetPhotoAtomically(photoFile)) {
        return Uri.fromFile(photoFile).toString()
    }

    val bytes = contentResolver.openInputStream(sourceUri)?.use { inputStream ->
        normalizePetPhotoBytes(inputStream.readBytes(), photoUri.contentTypeFromExtension())
    } ?: error("Не удалось открыть выбранное фото")
    writePetPhotoAtomically(photoFile) { it.write(bytes) }

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

private fun String.contentTypeFromExtension(): String = if (Uri.parse(this).path.orEmpty().lowercase().endsWith(".png")) {
    PNG_CONTENT_TYPE
} else {
    JPEG_CONTENT_TYPE
}

private fun String.fileUriExtension(): String {
    val path = Uri.parse(this).path.orEmpty().lowercase()
    return if (path.endsWith(".png")) ".png" else ".jpg"
}

private fun String.safeFilePart(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

internal fun decodePetPhotoFile(path: String): Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.onFailure { Log.w("IviPhoto", "ImageDecoder failed for $path", it) }.getOrNull()
} else {
    BitmapFactory.decodeFile(path)
}

private fun normalizePetPhotoBytes(bytes: ByteArray, contentType: String): ByteArray {
    val bitmap = decodePetPhotoBytes(bytes) ?: return bytes
    return ByteArrayOutputStream().use { outputStream ->
        val format = if (contentType.lowercase().substringBefore(';').trim() == PNG_CONTENT_TYPE) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(format, JPEG_QUALITY, outputStream)
        outputStream.toByteArray()
    }
}

private fun decodePetPhotoBytes(bytes: ByteArray): Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()
} else {
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

internal fun writePetPhotoAtomically(target: File, write: (OutputStream) -> Unit) {
    target.parentFile?.mkdirs()
    val tempFile = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
    try {
        tempFile.outputStream().use { outputStream ->
            write(outputStream)
            outputStream.flush()
        }
        check(tempFile.moveToPetPhotoAtomically(target)) { "Не удалось сохранить фото питомца" }
    } catch (throwable: Throwable) {
        tempFile.delete()
        throw throwable
    }
}

private fun File.moveToPetPhotoAtomically(target: File): Boolean {
    target.parentFile?.mkdirs()
    return runCatching {
        try {
            Files.move(toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(toPath(), target.toPath(), REPLACE_EXISTING)
        }
        true
    }.getOrDefault(false)
}

private const val JPEG_CONTENT_TYPE = "image/jpeg"
private const val PNG_CONTENT_TYPE = "image/png"
private const val JPEG_QUALITY = 90
