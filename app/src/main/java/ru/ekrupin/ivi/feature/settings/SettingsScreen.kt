package ru.ekrupin.ivi.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var refreshTick by remember { mutableIntStateOf(0) }
    var firstEnabled by remember { mutableStateOf(true) }
    var firstDays by remember { mutableStateOf("7") }
    var secondEnabled by remember { mutableStateOf(true) }
    var secondDays by remember { mutableStateOf("2") }
    var firstDaysError by remember { mutableStateOf(false) }
    var secondDaysError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshTick++
    }

    LaunchedEffect(settings) {
        settings?.let {
            firstEnabled = it.firstReminderEnabled
            firstDays = it.firstReminderDaysBefore.toString()
            secondEnabled = it.secondReminderEnabled
            secondDays = it.secondReminderDaysBefore.toString()
        }
    }

    val notificationStatusVersion = refreshTick
    val notificationsPermissionGranted = context.isNotificationPermissionGranted(notificationStatusVersion)
    val notificationsEnabledInSystem = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val notificationsReady = notificationsPermissionGranted && notificationsEnabledInSystem
    val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
        activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        false
    }

    ScreenScaffold(title = stringResource(R.string.settings_title)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_system_notifications_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (notificationsReady) {
                        stringResource(R.string.settings_system_notifications_ready)
                    } else {
                        stringResource(R.string.settings_system_notifications_blocked)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!notificationsReady) {
                    Text(
                        text = if (shouldShowRationale) {
                            stringResource(R.string.settings_system_notifications_rationale)
                        } else {
                            stringResource(R.string.settings_permission_denied)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsPermissionGranted) {
                            Button(onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) {
                                Text(stringResource(R.string.settings_request_permission))
                            }
                        }
                        Button(onClick = { context.openAppNotificationSettings() }) {
                            Text(stringResource(R.string.settings_open_system_settings))
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_schedule_preview),
                    style = MaterialTheme.typography.bodySmall,
                )

                ReminderRuleEditor(
                    title = stringResource(R.string.settings_first_enabled),
                    checked = firstEnabled,
                    onCheckedChange = { firstEnabled = it },
                    daysValue = firstDays,
                    onDaysChange = {
                        firstDays = it
                        firstDaysError = false
                    },
                    daysLabel = stringResource(R.string.settings_first_days),
                    isError = firstDaysError,
                )

                ReminderRuleEditor(
                    title = stringResource(R.string.settings_second_enabled),
                    checked = secondEnabled,
                    onCheckedChange = { secondEnabled = it },
                    daysValue = secondDays,
                    onDaysChange = {
                        secondDays = it
                        secondDaysError = false
                    },
                    daysLabel = stringResource(R.string.settings_second_days),
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
    }
}

@Composable
private fun ReminderRuleEditor(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    daysValue: String,
    onDaysChange: (String) -> Unit,
    daysLabel: String,
    isError: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        OutlinedTextField(
            value = daysValue,
            onValueChange = onDaysChange,
            label = { Text(daysLabel) },
            enabled = checked,
            supportingText = {
                if (isError) Text(stringResource(R.string.validation_number_invalid))
            },
            isError = isError,
        )
    }
}

private fun Context.isNotificationPermissionGranted(@Suppress("UNUSED_PARAMETER") version: Int): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
