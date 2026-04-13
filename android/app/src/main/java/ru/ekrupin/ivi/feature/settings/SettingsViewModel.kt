package ru.ekrupin.ivi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import java.time.LocalDateTime

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings?> = reminderSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveSettings(
        firstEnabled: Boolean,
        firstDays: Int,
        secondEnabled: Boolean,
        secondDays: Int,
    ) {
        val current = settings.value
        viewModelScope.launch {
            reminderSettingsRepository.saveSettings(
                ReminderSettings(
                    id = current?.id ?: AppConstants.REMINDER_SETTINGS_ID,
                    firstReminderEnabled = firstEnabled,
                    firstReminderDaysBefore = firstDays,
                    secondReminderEnabled = secondEnabled,
                    secondReminderDaysBefore = secondDays,
                    createdAt = current?.createdAt ?: LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }
}
