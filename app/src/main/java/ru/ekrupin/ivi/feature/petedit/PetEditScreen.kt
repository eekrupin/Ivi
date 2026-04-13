package ru.ekrupin.ivi.feature.petedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.DatePickerField
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.core.util.copyPickedPetPhoto
import ru.ekrupin.ivi.core.util.deleteManagedPetPhoto
import java.time.LocalDate

@Composable
fun PetEditScreen(
    onSaved: () -> Unit,
    viewModel: PetEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable(uiState.pet?.id) { mutableStateOf(false) }
    var originalPhotoUri by rememberSaveable(uiState.pet?.id) { mutableStateOf<String?>(null) }
    var saveCommitted by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.pet) {
        if (!initialized) {
            name = uiState.pet?.name.orEmpty()
            birthDate = uiState.pet?.birthDate
            photoUri = uiState.pet?.photoUri
            originalPhotoUri = uiState.pet?.photoUri
            initialized = true
        }
    }

    LaunchedEffect(uiState.saveCompletedTick) {
        if (uiState.saveCompletedTick > 0) onSaved()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.copyPickedPetPhoto(uri) }
            .onSuccess {
                if (photoUri != originalPhotoUri) {
                    context.deleteManagedPetPhoto(photoUri)
                }
                photoUri = it
            }
    }

    ScreenScaffold(title = stringResource(R.string.pet_edit_screen_title)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            ),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.pet_edit_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                EditablePetPhoto(
                    photoUri = photoUri,
                    petName = name.ifBlank { uiState.pet?.name ?: stringResource(R.string.app_name) },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.pet_name_label)) },
                    supportingText = {
                        Text(
                            if (nameError) stringResource(R.string.validation_name_required)
                            else stringResource(R.string.pet_name_hint),
                        )
                    },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                DatePickerField(
                    label = stringResource(R.string.pet_birth_date_label),
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    supportingText = stringResource(R.string.pet_birth_date_hint),
                    allowClear = true,
                    onClear = { birthDate = null },
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Text(
                            if (photoUri.isNullOrBlank()) stringResource(R.string.pet_photo_pick)
                            else stringResource(R.string.pet_photo_replace),
                        )
                    }

                    if (!photoUri.isNullOrBlank()) {
                        OutlinedButton(onClick = {
                            if (photoUri != originalPhotoUri) {
                                context.deleteManagedPetPhoto(photoUri)
                            }
                            photoUri = null
                        }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Text(stringResource(R.string.pet_photo_delete))
                        }
                    }
                }
            }
        }

        RowActions(
            onSave = {
                val normalizedName = name.trim()
                nameError = normalizedName.isBlank()
                if (!nameError) {
                    saveCommitted = true
                    if (originalPhotoUri != photoUri) {
                        context.deleteManagedPetPhoto(originalPhotoUri)
                    }
                    viewModel.savePet(normalizedName, birthDate, photoUri)
                }
            },
            onCancel = {
                if (!saveCommitted && photoUri != originalPhotoUri) {
                    context.deleteManagedPetPhoto(photoUri)
                }
                onSaved()
            },
        )
    }
}

@Composable
private fun EditablePetPhoto(
    photoUri: String?,
    petName: String,
    onPickPhoto: () -> Unit,
) {
    if (photoUri.isNullOrBlank()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickPhoto),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                }
                Text(
                    text = stringResource(R.string.pet_photo_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.pet_photo_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    AsyncImage(
        model = photoUri,
        contentDescription = stringResource(R.string.pet_photo_content_description, petName),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .size(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onPickPhoto),
    )
}

@Composable
private fun RowActions(
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onSave) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Text(stringResource(R.string.common_save))
        }
        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.common_cancel))
        }
    }
}
