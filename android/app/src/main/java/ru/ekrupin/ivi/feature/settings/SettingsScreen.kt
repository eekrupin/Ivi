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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold

@Composable
fun SettingsScreen(
    onOpenConflicts: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = context.findActivity()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncUiState by viewModel.syncUiState.collectAsStateWithLifecycle()
    var refreshTick by remember { mutableIntStateOf(0) }
    var firstEnabled by remember { mutableStateOf(true) }
    var firstDays by remember { mutableStateOf("7") }
    var secondEnabled by remember { mutableStateOf(true) }
    var secondDays by remember { mutableStateOf("2") }
    var firstDaysError by remember { mutableStateOf(false) }
    var secondDaysError by remember { mutableStateOf(false) }
    var previousSyncStatus by remember { mutableStateOf(syncUiState.status) }

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

    LaunchedEffect(syncUiState.isConnected, syncUiState.status) {
        val isSuccessfulSync = syncUiState.status.allowsForcedPetAccessRefresh()
        val isNewSuccessfulSync = isSuccessfulSync && previousSyncStatus != syncUiState.status
        previousSyncStatus = syncUiState.status
        val shouldRefreshPetAccess = syncUiState.isConnected &&
            ((syncUiState.petAccess is PetAccessUiState.Unknown && syncUiState.status.allowsPetAccessRefresh()) ||
                (syncUiState.petAccess is PetAccessUiState.NoServerPet && isNewSuccessfulSync))
        if (shouldRefreshPetAccess) viewModel.refreshCurrentPetAccess(force = isNewSuccessfulSync)
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f),
            ),
        ) {
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
                            FilledTonalButton(onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) {
                                Text(stringResource(R.string.settings_request_permission))
                            }
                        }
                        OutlinedButton(onClick = { context.openAppNotificationSettings() }) {
                            Text(stringResource(R.string.settings_open_system_settings))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_sync_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_sync_description),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = syncUiState.baseUrl,
                    onValueChange = viewModel::updateSyncBaseUrl,
                    label = { Text(stringResource(R.string.settings_sync_base_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                when (val connection = syncUiState.connectionStatus) {
                    ConnectionStatus.NotConfigured -> {
                        Text(
                            text = stringResource(R.string.settings_sync_not_configured),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is ConnectionStatus.NotConnected -> {
                        Text(
                            text = stringResource(R.string.settings_sync_not_connected, connection.backendUrl),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    ConnectionStatus.Loading -> {
                        Text(
                            text = stringResource(R.string.settings_sync_connecting),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is ConnectionStatus.Connected -> {
                        Text(
                            text = stringResource(
                                R.string.settings_sync_connected,
                                connection.displayName ?: connection.email,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is ConnectionStatus.Error -> {
                        Text(
                            text = connection.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (!syncUiState.isConnected) {
                    OutlinedTextField(
                        value = syncUiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text(stringResource(R.string.settings_sync_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = syncUiState.displayName,
                        onValueChange = viewModel::updateDisplayName,
                        label = { Text(stringResource(R.string.settings_sync_display_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = syncUiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text(stringResource(R.string.settings_sync_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                Text(
                    text = syncUiState.status.label(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (syncUiState.status) {
                        SyncStatus.NotConfigured -> MaterialTheme.colorScheme.onSurfaceVariant
                        SyncStatus.Success -> MaterialTheme.colorScheme.primary
                        SyncStatus.ForegroundSuccess -> MaterialTheme.colorScheme.primary
                        SyncStatus.Conflicts -> MaterialTheme.colorScheme.tertiary
                        SyncStatus.RequiresBootstrap -> MaterialTheme.colorScheme.error
                        SyncStatus.NoServerPet -> MaterialTheme.colorScheme.tertiary
                        is SyncStatus.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (syncUiState.status == SyncStatus.RequiresBootstrap || syncUiState.status == SyncStatus.NoServerPet) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (syncUiState.status == SyncStatus.NoServerPet) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = if (syncUiState.status == SyncStatus.NoServerPet) {
                                    stringResource(R.string.settings_sync_no_server_pet_title)
                                } else {
                                    stringResource(R.string.settings_sync_bootstrap_recovery_title)
                                },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = if (syncUiState.status == SyncStatus.NoServerPet) {
                                    stringResource(R.string.settings_sync_no_server_pet_body)
                                } else {
                                    stringResource(R.string.settings_sync_bootstrap_recovery_body)
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = viewModel::publishLocalDataToServer,
                                    enabled = syncUiState.isConnected && syncUiState.status != SyncStatus.Running,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.settings_sync_publish_local))
                                }
                                if (syncUiState.status == SyncStatus.RequiresBootstrap) {
                                    OutlinedButton(
                                        onClick = viewModel::replaceLocalDataFromServer,
                                        enabled = syncUiState.isConnected && syncUiState.status != SyncStatus.Running,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.settings_sync_replace_from_server))
                                    }
                                }
                            }
                            if (syncUiState.status == SyncStatus.RequiresBootstrap) {
                                Text(
                                    text = stringResource(R.string.settings_sync_replace_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                if (syncUiState.conflictCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_sync_conflicts_title, syncUiState.conflictCount),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.settings_sync_conflicts_body),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(onClick = onOpenConflicts) {
                                Text(stringResource(R.string.settings_sync_conflicts_open))
                            }
                        }
                    }
                }
                if (syncUiState.isConnected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_invite_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.settings_invite_body),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            when (val petAccess = syncUiState.petAccess) {
                                PetAccessUiState.Unknown -> Text(
                                    text = stringResource(R.string.settings_pet_access_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                PetAccessUiState.Loading -> Text(
                                    text = stringResource(R.string.settings_pet_access_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                PetAccessUiState.NoServerPet -> Text(
                                    text = stringResource(R.string.settings_pet_access_no_server_pet),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                is PetAccessUiState.Known -> {
                                    Text(
                                        text = stringResource(R.string.settings_pet_access_pet, petAccess.petName),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_pet_access_role, petAccess.role.label()),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            if ((syncUiState.petAccess as? PetAccessUiState.Known)?.role == PetAccessRole.Owner) {
                                FilledTonalButton(
                                    onClick = viewModel::createInvite,
                                    enabled = syncUiState.status != SyncStatus.Running && syncUiState.inviteStatus != InviteStatus.Loading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.settings_invite_create))
                                }
                                OutlinedButton(
                                    onClick = viewModel::leaveSharedPet,
                                    enabled = syncUiState.status != SyncStatus.Running && syncUiState.leavePetStatus != LeavePetStatus.Loading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.settings_leave_owner_pet))
                                }
                            } else if ((syncUiState.petAccess as? PetAccessUiState.Known)?.role == PetAccessRole.Member) {
                                Text(
                                    text = stringResource(R.string.settings_invite_owner_only),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                OutlinedButton(
                                    onClick = viewModel::leaveSharedPet,
                                    enabled = syncUiState.status != SyncStatus.Running && syncUiState.leavePetStatus != LeavePetStatus.Loading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.settings_leave_shared_pet))
                                }
                            }
                            OutlinedTextField(
                                value = syncUiState.inviteCode,
                                onValueChange = viewModel::updateInviteCode,
                                label = { Text(stringResource(R.string.settings_invite_code)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedButton(
                                onClick = viewModel::acceptInvite,
                                enabled = syncUiState.inviteCode.isNotBlank() && syncUiState.inviteStatus != InviteStatus.Loading && syncUiState.leavePetStatus != LeavePetStatus.Loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.settings_invite_accept))
                            }
                            when (val leavePetStatus = syncUiState.leavePetStatus) {
                                LeavePetStatus.Idle -> Unit
                                LeavePetStatus.Loading -> Text(
                                    text = stringResource(R.string.settings_leave_shared_pet_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                LeavePetStatus.Left -> Text(
                                    text = stringResource(R.string.settings_leave_shared_pet_success),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                is LeavePetStatus.TransferRequired -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.settings_leave_owner_transfer_required),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    leavePetStatus.candidates.forEach { candidate ->
                                        OutlinedButton(
                                            onClick = { viewModel.transferOwnerAndLeave(candidate.id) },
                                            enabled = syncUiState.status != SyncStatus.Running,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(candidate.label())
                                        }
                                    }
                                }
                                LeavePetStatus.DeletePetConfirmation -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.settings_leave_owner_delete_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    OutlinedButton(
                                        onClick = viewModel::deletePetAndLeave,
                                        enabled = syncUiState.status != SyncStatus.Running,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.settings_leave_owner_delete_confirm))
                                    }
                                }
                                is LeavePetStatus.Error -> Text(
                                    text = leavePetStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            when (val inviteStatus = syncUiState.inviteStatus) {
                                InviteStatus.Idle -> Unit
                                InviteStatus.Loading -> Text(
                                    text = stringResource(R.string.settings_invite_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                is InviteStatus.Created -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SelectionContainer {
                                        Text(
                                            text = stringResource(R.string.settings_invite_created, inviteStatus.code),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(inviteStatus.code))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.common_copy))
                                    }
                                }
                                is InviteStatus.Accepted -> Text(
                                    text = stringResource(
                                        R.string.settings_invite_accepted,
                                        inviteStatus.petName,
                                        inviteStatus.role.label(),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                is InviteStatus.Error -> Text(
                                    text = inviteStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                if (!syncUiState.isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = viewModel::login,
                            enabled = syncUiState.baseUrl.isNotBlank() && syncUiState.email.isNotBlank() && syncUiState.password.isNotBlank() && syncUiState.status != SyncStatus.Running,
                        ) {
                            Text(stringResource(R.string.settings_sync_login))
                        }
                        OutlinedButton(
                            onClick = viewModel::register,
                            enabled = syncUiState.baseUrl.isNotBlank() && syncUiState.email.isNotBlank() && syncUiState.password.isNotBlank() && syncUiState.displayName.isNotBlank() && syncUiState.status != SyncStatus.Running,
                        ) {
                            Text(stringResource(R.string.settings_sync_register))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::logout,
                        enabled = syncUiState.status != SyncStatus.Running,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_sync_logout))
                    }
                }
                FilledTonalButton(
                    onClick = viewModel::runSync,
                    enabled = syncUiState.isConnected && syncUiState.status != SyncStatus.Running,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (syncUiState.status == SyncStatus.Running) {
                            stringResource(R.string.settings_sync_running)
                        } else {
                            stringResource(R.string.settings_sync_run)
                        },
                    )
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

@Composable
private fun PetAccessRole.label(): String = when (this) {
    PetAccessRole.Owner -> stringResource(R.string.settings_pet_role_owner)
    PetAccessRole.Member -> stringResource(R.string.settings_pet_role_member)
    PetAccessRole.Unknown -> stringResource(R.string.settings_pet_role_unknown)
}

private fun PetOwnerTransferCandidate.label(): String = displayName?.takeIf { it.isNotBlank() }?.let { "$it ($email)" } ?: email

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun SyncStatus.label(context: Context): String = when (this) {
    SyncStatus.NotConfigured -> context.getString(R.string.settings_sync_not_configured)
    SyncStatus.Idle -> context.getString(R.string.settings_sync_idle)
    SyncStatus.Running -> context.getString(R.string.settings_sync_running)
    SyncStatus.Success -> context.getString(R.string.settings_sync_success)
    SyncStatus.ForegroundSuccess -> context.getString(R.string.settings_sync_foreground_success)
    SyncStatus.Conflicts -> context.getString(R.string.settings_sync_conflicts)
    SyncStatus.RequiresBootstrap -> context.getString(R.string.settings_sync_requires_bootstrap)
    SyncStatus.NoServerPet -> context.getString(R.string.settings_sync_no_server_pet)
    is SyncStatus.Error -> message
}

private fun SyncStatus.allowsPetAccessRefresh(): Boolean = when (this) {
    SyncStatus.Idle,
    SyncStatus.Success,
    SyncStatus.ForegroundSuccess -> true
    SyncStatus.NotConfigured,
    SyncStatus.Running,
    SyncStatus.Conflicts,
    SyncStatus.RequiresBootstrap,
    SyncStatus.NoServerPet,
    is SyncStatus.Error -> false
}

private fun SyncStatus.allowsForcedPetAccessRefresh(): Boolean = when (this) {
    SyncStatus.Success,
    SyncStatus.ForegroundSuccess -> true
    SyncStatus.NotConfigured,
    SyncStatus.Idle,
    SyncStatus.Running,
    SyncStatus.Conflicts,
    SyncStatus.RequiresBootstrap,
    SyncStatus.NoServerPet,
    is SyncStatus.Error -> false
}
