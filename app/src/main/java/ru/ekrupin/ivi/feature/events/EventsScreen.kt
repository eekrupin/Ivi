package ru.ekrupin.ivi.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.domain.model.PetEventStatus

@Composable
fun EventsScreen(
    onCreateEvent: () -> Unit,
    onEditEvent: (Long) -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.events_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                PetEventStatus.ACTIVE,
                PetEventStatus.COMPLETED,
                PetEventStatus.ARCHIVED,
            ).forEach { status ->
                FilterChip(
                    selected = uiState.filter == status,
                    onClick = { viewModel.setFilter(status) },
                    label = {
                        Text(
                            when (status) {
                                PetEventStatus.ACTIVE -> stringResource(R.string.events_filter_active)
                                PetEventStatus.COMPLETED -> stringResource(R.string.events_filter_completed)
                                PetEventStatus.ARCHIVED -> stringResource(R.string.events_filter_archived)
                            },
                        )
                    },
                )
            }
        }

        if (uiState.items.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.events_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        uiState.items.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (item.status) {
                        PetEventStatus.ACTIVE -> MaterialTheme.colorScheme.surface
                        PetEventStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                        PetEventStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = when (item.status) {
                                    PetEventStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                    PetEventStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                                    PetEventStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Text(
                                    text = when (item.status) {
                                        PetEventStatus.ACTIVE -> stringResource(R.string.events_filter_active)
                                        PetEventStatus.COMPLETED -> stringResource(R.string.events_filter_completed)
                                        PetEventStatus.ARCHIVED -> stringResource(R.string.events_filter_archived)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = item.subtitle, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = when (item.reminderState) {
                                ReminderStateUi.ENABLED -> stringResource(R.string.events_notifications_on)
                                ReminderStateUi.DISABLED -> stringResource(R.string.events_notifications_off)
                                ReminderStateUi.INACTIVE_STATUS -> stringResource(R.string.events_notifications_inactive_status)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onEditEvent(item.id) }) {
                            Text(stringResource(R.string.events_edit))
                        }
                        when (item.status) {
                            PetEventStatus.ACTIVE -> {
                                TextButton(onClick = { viewModel.updateStatus(item.id, PetEventStatus.COMPLETED) }) {
                                    Text(stringResource(R.string.events_mark_completed))
                                }
                                TextButton(onClick = { viewModel.updateStatus(item.id, PetEventStatus.ARCHIVED) }) {
                                    Text(stringResource(R.string.events_mark_archived))
                                }
                            }

                            PetEventStatus.COMPLETED,
                            PetEventStatus.ARCHIVED,
                            -> {
                                TextButton(onClick = { viewModel.updateStatus(item.id, PetEventStatus.ACTIVE) }) {
                                    Text(stringResource(R.string.events_mark_active))
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onCreateEvent) {
            Text(text = stringResource(R.string.home_add_event))
        }
    }
}
