package ru.ekrupin.ivi.feature.syncconflicts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictListItem
import ru.ekrupin.ivi.data.sync.model.SyncEntityType

@Composable
fun SyncConflictsScreen(viewModel: SyncConflictsViewModel = hiltViewModel()) {
    val conflicts = viewModel.conflicts.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.sync_conflicts_title)) {
        Text(
            text = stringResource(R.string.sync_conflicts_description),
            style = MaterialTheme.typography.bodyMedium,
        )

        if (conflicts.value.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sync_conflicts_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.sync_conflicts_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            conflicts.value.forEach { item ->
                ConflictCard(
                    item = item,
                    onAcceptServer = { viewModel.acceptServerVersion(item.id) },
                    onRetryLocal = { viewModel.retryLocalChanges(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ConflictCard(
    item: SyncConflictListItem,
    onAcceptServer: () -> Unit,
    onRetryLocal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.entityType.toLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = item.reasonText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.sync_conflicts_conflicted_at, item.conflictedAt.toDisplayDateTime()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!item.hasServerSnapshot) {
                Text(
                    text = stringResource(R.string.sync_conflicts_no_snapshot_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onAcceptServer) {
                    Text(stringResource(R.string.sync_conflicts_accept_server))
                }
                OutlinedButton(onClick = onRetryLocal) {
                    Text(stringResource(R.string.sync_conflicts_retry_local))
                }
            }
        }
    }
}

private fun SyncEntityType.toLabel(): String = when (this) {
    SyncEntityType.EVENT_TYPE -> "Тип события"
    SyncEntityType.PET_EVENT -> "Событие"
    SyncEntityType.WEIGHT_ENTRY -> "Вес"
    else -> "Запись"
}

private fun LocalDateTime.toDisplayDateTime(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
