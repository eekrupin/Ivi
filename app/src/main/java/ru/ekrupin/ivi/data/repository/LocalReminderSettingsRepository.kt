package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.dao.ReminderSettingsDao
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.mapper.toEntity
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository

class LocalReminderSettingsRepository @Inject constructor(
    private val reminderSettingsDao: ReminderSettingsDao,
    private val reminderScheduler: ReminderScheduler,
) : ReminderSettingsRepository {
    override fun observeSettings(): Flow<ReminderSettings?> = reminderSettingsDao.observe().map { it?.toDomain() }

    override suspend fun saveSettings(settings: ReminderSettings) {
        val now = LocalDateTime.now()
        reminderSettingsDao.insert(
            settings.copy(
                id = AppConstants.REMINDER_SETTINGS_ID,
                createdAt = if (settings.id == 0L) now else settings.createdAt,
                updatedAt = now,
            ).toEntity(),
        )
        reminderScheduler.refreshAll()
    }
}
