package ru.ekrupin.ivi.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var firstEnabled by remember { mutableStateOf(true) }
    var firstDays by remember { mutableStateOf("7") }
    var secondEnabled by remember { mutableStateOf(true) }
    var secondDays by remember { mutableStateOf("2") }
    var firstDaysError by remember { mutableStateOf(false) }
    var secondDaysError by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        settings?.let {
            firstEnabled = it.firstReminderEnabled
            firstDays = it.firstReminderDaysBefore.toString()
            secondEnabled = it.secondReminderEnabled
            secondDays = it.secondReminderDaysBefore.toString()
        }
    }

    ScreenScaffold(title = stringResource(R.string.settings_title)) {
        Text(stringResource(R.string.settings_description))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_first_enabled))
            Switch(checked = firstEnabled, onCheckedChange = { firstEnabled = it })
        }
        OutlinedTextField(
            value = firstDays,
            onValueChange = {
                firstDays = it
                firstDaysError = false
            },
            label = { Text(stringResource(R.string.settings_first_days)) },
            supportingText = {
                if (firstDaysError) Text(stringResource(R.string.validation_number_invalid))
            },
            isError = firstDaysError,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_second_enabled))
            Switch(checked = secondEnabled, onCheckedChange = { secondEnabled = it })
        }
        OutlinedTextField(
            value = secondDays,
            onValueChange = {
                secondDays = it
                secondDaysError = false
            },
            label = { Text(stringResource(R.string.settings_second_days)) },
            supportingText = {
                if (secondDaysError) Text(stringResource(R.string.validation_number_invalid))
            },
            isError = secondDaysError,
        )
        Button(onClick = {
            val first = firstDays.toIntOrNull()
            val second = secondDays.toIntOrNull()
            firstDaysError = first == null
            secondDaysError = second == null
            if (first != null && second != null) {
                viewModel.saveSettings(firstEnabled, first, secondEnabled, second)
            }
        }) {
            Text(stringResource(R.string.common_save))
        }
    }
}
