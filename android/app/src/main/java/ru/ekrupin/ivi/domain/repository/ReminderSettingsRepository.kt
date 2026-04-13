package ru.ekrupin.ivi.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.domain.model.ReminderSettings

interface ReminderSettingsRepository {
    fun observeSettings(): Flow<ReminderSettings?>

    suspend fun saveSettings(settings: ReminderSettings)
}
