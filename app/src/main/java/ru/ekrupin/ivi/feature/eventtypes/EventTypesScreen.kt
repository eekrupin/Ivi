package ru.ekrupin.ivi.feature.eventtypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.core.util.labelRes
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.EventType

@Composable
fun EventTypesScreen(viewModel: EventTypesViewModel = hiltViewModel()) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    var editingType by remember { mutableStateOf<EventType?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.event_types_title)) {
        Button(
            onClick = {
                editingType = null
                showDialog = true
            },
        ) {
            Text(stringResource(R.string.event_type_add))
        }

        if (types.isEmpty()) {
            Text(stringResource(R.string.event_types_empty))
        }

        types.forEach { type ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = type.name)
                    Text(text = stringResource(type.category.labelRes()))
                    Text(
                        text = type.defaultDurationDays?.let { "Автоконтроль через $it дн." }
                            ?: stringResource(R.string.event_type_no_duration),
                    )
                    Text(
                        text = if (type.isActive) stringResource(R.string.common_enabled) else stringResource(R.string.common_disabled),
                    )
                    TextButton(onClick = {
                        editingType = type
                        showDialog = true
                    }) {
                        Text(stringResource(R.string.common_edit))
                    }
                }
            }
        }
    }

    if (showDialog) {
        EventTypeDialog(
            initialType = editingType,
            onDismiss = { showDialog = false },
            onSave = { id, name, category, durationDays, isActive ->
                viewModel.saveType(id, name, category, durationDays, isActive)
                showDialog = false
            },
        )
    }
}

@Composable
private fun EventTypeDialog(
    initialType: EventType?,
    onDismiss: () -> Unit,
    onSave: (Long, String, EventCategory, Int?, Boolean) -> Unit,
) {
    var name by remember(initialType) { mutableStateOf(initialType?.name.orEmpty()) }
    var duration by remember(initialType) { mutableStateOf(initialType?.defaultDurationDays?.toString().orEmpty()) }
    var active by remember(initialType) { mutableStateOf(initialType?.isActive ?: true) }
    var selectedCategory by remember(initialType) { mutableStateOf(initialType?.category ?: EventCategory.OTHER) }
    var showNameError by remember { mutableStateOf(false) }
    var showDurationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialType == null) stringResource(R.string.event_type_add)
                else stringResource(R.string.event_type_edit),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showNameError = false
                    },
                    label = { Text(stringResource(R.string.event_type_name_label)) },
                    supportingText = {
                        if (showNameError) Text(stringResource(R.string.validation_name_required))
                    },
                    isError = showNameError,
                )
                Text(stringResource(R.string.event_type_category_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventCategory.entries.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            label = { Text(stringResource(category.labelRes())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = {
                        duration = it
                        showDurationError = false
                    },
                    label = { Text(stringResource(R.string.event_type_duration_label)) },
                    supportingText = {
                        if (showDurationError) Text(stringResource(R.string.validation_number_invalid))
                    },
                    isError = showDurationError,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.event_type_active_label))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDuration = duration.takeIf { it.isNotBlank() }?.toIntOrNull()
                when {
                    name.isBlank() -> showNameError = true
                    duration.isNotBlank() && parsedDuration == null -> showDurationError = true
                    else -> onSave(initialType?.id ?: 0L, name.trim(), selectedCategory, parsedDuration, active)
                }
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
