package ru.ekrupin.ivi.feature.eventedit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.core.util.parseDisplayDate
import ru.ekrupin.ivi.core.util.toDisplayDate

@Composable
fun EventEditScreen(
    onSaved: () -> Unit,
    viewModel: EventEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTypeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var eventDate by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var typeError by rememberSaveable { mutableStateOf(false) }
    var eventDateError by rememberSaveable { mutableStateOf(false) }
    var dueDateError by rememberSaveable { mutableStateOf(false) }
    var initialized by rememberSaveable(uiState.existingEvent?.id) { mutableStateOf(false) }

    LaunchedEffect(uiState.existingEvent, uiState.eventTypes) {
        if (!initialized && uiState.eventTypes.isNotEmpty()) {
            val existing = uiState.existingEvent
            selectedTypeId = existing?.eventTypeId ?: uiState.eventTypes.first().id
            eventDate = existing?.eventDate?.toDisplayDate() ?: java.time.LocalDate.now().toDisplayDate()
            dueDate = existing?.dueDate?.toDisplayDate().orEmpty()
            comment = existing?.comment.orEmpty()
            notificationsEnabled = existing?.notificationsEnabled ?: true
            initialized = true
        }
    }

    LaunchedEffect(uiState.saveCompletedTick) {
        if (uiState.saveCompletedTick > 0) onSaved()
    }

    val selectedType = uiState.eventTypes.firstOrNull { it.id == selectedTypeId }

    ScreenScaffold(
        title = stringResource(
            if (uiState.existingEvent == null) R.string.event_edit_title_new
            else R.string.event_edit_title_existing,
        ),
    ) {
        if (uiState.eventTypes.isEmpty()) {
            Text(stringResource(R.string.event_no_types))
            return@ScreenScaffold
        }

        Text(stringResource(R.string.event_type_label))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = androidx.compose.ui.Modifier.horizontalScroll(rememberScrollState()),
        ) {
            uiState.eventTypes.forEach { type ->
                FilterChip(
                    selected = selectedTypeId == type.id,
                    onClick = {
                        selectedTypeId = type.id
                        typeError = false
                    },
                    label = { Text(type.name) },
                )
            }
        }
        if (typeError) {
            Text(stringResource(R.string.validation_type_required))
        }

        OutlinedTextField(
            value = eventDate,
            onValueChange = {
                eventDate = it
                eventDateError = false
            },
            label = { Text(stringResource(R.string.event_date_label)) },
            supportingText = {
                Text(
                    if (eventDateError) stringResource(R.string.validation_date_invalid)
                    else stringResource(R.string.common_format_date),
                )
            },
            isError = eventDateError,
        )
        OutlinedTextField(
            value = dueDate,
            onValueChange = {
                dueDate = it
                dueDateError = false
            },
            label = { Text(stringResource(R.string.event_due_date_label)) },
            supportingText = {
                Text(
                    if (dueDateError) stringResource(R.string.validation_date_invalid)
                    else stringResource(R.string.event_due_date_hint),
                )
            },
            isError = dueDateError,
        )
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text(stringResource(R.string.event_comment_label)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.event_notifications_label))
            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
        }
        Button(onClick = {
            val parsedEventDate = parseDisplayDate(eventDate)
            val parsedDueDate = when {
                dueDate.isBlank() -> null
                else -> parseDisplayDate(dueDate)
            }
            typeError = selectedTypeId == null
            eventDateError = parsedEventDate == null
            dueDateError = dueDate.isNotBlank() && parsedDueDate == null
            if (!typeError && parsedEventDate != null && !dueDateError) {
                viewModel.saveEvent(
                    selectedTypeId = selectedTypeId!!,
                    eventDate = parsedEventDate,
                    dueDate = parsedDueDate,
                    comment = comment.trim(),
                    notificationsEnabled = notificationsEnabled,
                    defaultDurationDays = selectedType?.defaultDurationDays,
                )
            }
        }) {
            Text(stringResource(R.string.common_save))
        }
    }
}
