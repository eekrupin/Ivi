package ru.ekrupin.ivi.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.DatePickerField
import ru.ekrupin.ivi.core.ui.InfoCard
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.core.util.toDisplayDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    onAddEvent: () -> Unit,
    onOpenWeight: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var photoUri by remember { mutableStateOf("") }

    ScreenScaffold(title = stringResource(R.string.nav_home)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Pets, contentDescription = null)
                    Column {
                        Text(
                            text = uiState.petName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(text = uiState.birthDateLabel, style = MaterialTheme.typography.bodyMedium)
                        uiState.photoUri?.takeIf { it.isNotBlank() }?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(
                    onClick = {
                        name = uiState.petName
                        birthDate = uiState.birthDate
                        photoUri = uiState.photoUri.orEmpty()
                        showEditDialog = true
                    },
                ) {
                    Text(stringResource(R.string.home_edit_pet))
                }
            }
        }

        InfoCard(
            title = stringResource(R.string.home_current_weight),
            body = uiState.currentWeightLabel,
        )
        InfoCard(
            title = stringResource(R.string.home_next_reminder),
            body = uiState.nextReminderLabel,
        )
        InfoCard(
            title = stringResource(R.string.home_active_events),
            body = uiState.activeEvents.ifEmpty { listOf(stringResource(R.string.home_no_active_events)) }.joinToString("\n"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAddEvent) {
                Text(text = stringResource(R.string.home_add_event))
            }
            Button(onClick = onOpenWeight) {
                Text(text = stringResource(R.string.home_add_weight))
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.pet_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.pet_name_label)) },
                        singleLine = true,
                    )
                    DatePickerField(
                        label = stringResource(R.string.pet_birth_date_label),
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        supportingText = stringResource(R.string.common_pick_date),
                        allowClear = true,
                        onClear = { birthDate = null },
                    )
                    OutlinedTextField(
                        value = photoUri,
                        onValueChange = { photoUri = it },
                        label = { Text(stringResource(R.string.pet_photo_uri_label)) },
                        supportingText = { Text(stringResource(R.string.pet_photo_uri_hint)) },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.savePet(name = name.trim(), birthDate = birthDate, photoUri = photoUri.trim())
                        showEditDialog = false
                    },
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
