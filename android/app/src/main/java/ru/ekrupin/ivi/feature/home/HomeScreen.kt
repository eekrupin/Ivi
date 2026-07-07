package ru.ekrupin.ivi.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.IviTestTags
import ru.ekrupin.ivi.core.ui.LocalPetPhotoImage
import ru.ekrupin.ivi.core.ui.ScreenScaffold

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun HomeScreen(
    onAddEvent: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConflicts: () -> Unit,
    onEditPet: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(R.string.nav_home),
        modifier = Modifier.testTag(IviTestTags.HOME_ROOT),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(IviTestTags.HOME_PET_OVERVIEW),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    PetPhoto(
                        photoUri = uiState.photoUri,
                        petName = uiState.petName,
                        onPickPhoto = onEditPet,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = uiState.petName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        ) {
                            Text(
                                text = uiState.ageLabel,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        Text(
                            text = uiState.birthDateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = onEditPet,
                            modifier = Modifier.testTag(IviTestTags.HOME_EDIT_PET_BUTTON),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Text(stringResource(R.string.common_edit))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.MonitorWeight,
                        title = stringResource(R.string.home_current_weight),
                        value = uiState.currentWeightLabel,
                    )
                    HomeMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.NotificationsActive,
                        title = stringResource(R.string.home_next_highlight),
                        value = uiState.nextHighlight.title,
                        supporting = uiState.nextHighlight.subtitle,
                    )
                }
            }
        }

        uiState.conflictBanner?.let { banner ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(IviTestTags.HOME_CONFLICT_CARD),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null)
                        Column {
                            Text(
                                text = stringResource(R.string.home_conflicts_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.home_conflicts_count, banner.count),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.home_conflicts_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(
                        onClick = onOpenConflicts,
                        modifier = Modifier.testTag(IviTestTags.HOME_OPEN_CONFLICTS_BUTTON),
                    ) {
                        Text(stringResource(R.string.home_conflicts_open))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Column {
                        Text(
                            text = stringResource(R.string.home_next_highlight_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = uiState.nextHighlight.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = uiState.nextHighlight.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Text(
                        text = stringResource(R.string.home_active_events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (uiState.activeEvents.isEmpty()) {
                    HomeEmptyState(
                        title = stringResource(R.string.home_no_active_events_title),
                        body = stringResource(R.string.home_no_active_events),
                        modifier = Modifier.testTag(IviTestTags.HOME_NO_ACTIVE_EVENTS),
                    )
                } else {
                    uiState.activeEvents.forEach { item ->
                        HomeEventRow(item = item)
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeQuickAction(
                icon = Icons.Outlined.Pets,
                label = stringResource(R.string.nav_events),
                onClick = onOpenEvents,
            )
            HomeQuickAction(
                icon = Icons.Outlined.FitnessCenter,
                label = stringResource(R.string.home_add_event),
                onClick = onAddEvent,
                modifier = Modifier.testTag(IviTestTags.HOME_ADD_EVENT_BUTTON),
            )
            HomeQuickAction(
                icon = Icons.Outlined.MonitorWeight,
                label = stringResource(R.string.nav_weight),
                onClick = onOpenWeight,
                modifier = Modifier.testTag(IviTestTags.HOME_OPEN_WEIGHT_BUTTON),
            )
            HomeQuickAction(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.nav_settings),
                onClick = onOpenSettings,
                modifier = Modifier.testTag(IviTestTags.HOME_OPEN_SETTINGS_BUTTON),
            )
        }
    }
}

@Composable
private fun PetPhoto(
    photoUri: String?,
    petName: String,
    onPickPhoto: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(116.dp)
            .testTag(IviTestTags.HOME_PET_PHOTO),
    ) {
        if (photoUri.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPickPhoto),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.pet_photo_placeholder),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            LocalPetPhotoImage(
                photoUri = photoUri,
                contentDescription = stringResource(R.string.pet_photo_content_description, petName),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(onClick = onPickPhoto),
            )
        }
    }
}

@Composable
private fun HomeMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    supporting: String? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = title, style = MaterialTheme.typography.labelLarge)
            }
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeEventRow(item: HomeEventItem) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier.width(150.dp)) {
        Icon(icon, contentDescription = null)
        Text(text = label)
    }
}

@Composable
private fun HomeEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
