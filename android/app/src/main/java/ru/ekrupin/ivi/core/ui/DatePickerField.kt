package ru.ekrupin.ivi.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.util.toDisplayDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    allowClear: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val zoneId = ZoneId.systemDefault()

    OutlinedTextField(
        value = value?.toDisplayDate().orEmpty(),
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        readOnly = true,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            Row {
                if (allowClear && value != null && onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_clear_date))
                    }
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.common_pick_date))
                }
            }
        },
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value
                ?.atStartOfDay(zoneId)
                ?.toInstant()
                ?.toEpochMilli(),
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())
                    }
                    showDialog = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
