package ru.ekrupin.ivi.core.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ekrupin.ivi.core.util.decodePetPhotoFile

@Composable
fun LocalPetPhotoImage(
    photoUri: String,
    contentDescription: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val parsedUri = remember(photoUri) { Uri.parse(photoUri) }
    if (parsedUri.scheme != "file") {
        AsyncImage(
            model = parsedUri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
        return
    }

    var imageBitmap by remember(photoUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        val filePath = parsedUri.path
        imageBitmap = withContext(Dispatchers.IO) {
            val bitmap = filePath?.let(::decodePetPhotoFile)
            Log.d("IviPhoto", "decode file=$filePath bitmap=${bitmap?.width}x${bitmap?.height}")
            bitmap?.asImageBitmap()
        }
    }

    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
