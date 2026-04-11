package ru.ekrupin.ivi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.core.util.toDisplayDate
import ru.ekrupin.ivi.core.util.toWeightLabel
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import ru.ekrupin.ivi.domain.repository.PetEventRepository
import ru.ekrupin.ivi.domain.repository.PetRepository
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import ru.ekrupin.ivi.domain.repository.WeightRepository
import java.time.LocalDate

data class HomeUiState(
    val petName: String = "",
    val birthDate: LocalDate? = null,
    val birthDateLabel: String = "",
    val photoUri: String? = null,
    val currentWeightLabel: String = "Пока нет записей",
    val nextReminderLabel: String = "Пока нет ближайших напоминаний",
    val activeEvents: List<String> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val weightRepository: WeightRepository,
    private val petEventRepository: PetEventRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val reminderSettingsRepository: ReminderSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        petRepository.observePet(),
        weightRepository.observeWeightHistory(),
        petEventRepository.observeEvents(),
        eventTypeRepository.observeTypes(),
        reminderSettingsRepository.observeSettings(),
    ) { pet, weights, events, types, settings ->
        val currentWeight = weights.firstOrNull()?.weightGrams?.toWeightLabel() ?: "Пока нет записей"
        val typeNames = types.associateBy({ it.id }, { it.name })
        val activeEvents = events
            .filter { it.status == PetEventStatus.ACTIVE }
            .sortedBy { it.dueDate ?: it.eventDate }
            .map { event ->
                val title = typeNames[event.eventTypeId] ?: "Событие"
                val suffix = event.dueDate?.let { "до ${it.toDisplayDate()}" } ?: event.eventDate.toDisplayDate()
                "$title • $suffix"
            }

        val nextReminder = settings?.let { reminder ->
            val candidates = events
                .filter { it.status == PetEventStatus.ACTIVE && it.notificationsEnabled && it.dueDate != null }
                .flatMap { event ->
                    buildList {
                        if (reminder.firstReminderEnabled) add(event to event.dueDate!!.minusDays(reminder.firstReminderDaysBefore.toLong()))
                        if (reminder.secondReminderEnabled) add(event to event.dueDate!!.minusDays(reminder.secondReminderDaysBefore.toLong()))
                    }
                }
                .filter { (_, reminderDate) -> reminderDate >= LocalDate.now() }
                .sortedBy { it.second }

            candidates.firstOrNull()?.let { (event, reminderDate) ->
                val title = typeNames[event.eventTypeId] ?: "Событие"
                "$title • ${reminderDate.toDisplayDate()}"
            }
        } ?: "Пока нет ближайших напоминаний"

        HomeUiState(
            petName = pet?.name ?: "Иви",
            birthDate = pet?.birthDate,
            birthDateLabel = pet?.birthDate?.toDisplayDate()?.let { "Дата рождения: $it" } ?: "Дату рождения можно добавить",
            photoUri = pet?.photoUri,
            currentWeightLabel = currentWeight,
            nextReminderLabel = nextReminder ?: "Пока нет ближайших напоминаний",
            activeEvents = activeEvents.take(3),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun savePet(name: String, birthDate: LocalDate?, photoUri: String?) {
        viewModelScope.launch {
            petRepository.savePet(name = name, birthDate = birthDate, photoUri = photoUri)
        }
    }
}
