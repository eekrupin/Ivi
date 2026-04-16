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
import ru.ekrupin.ivi.core.util.toAgeLabel
import ru.ekrupin.ivi.core.util.toWeightLabel
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictRepository
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import ru.ekrupin.ivi.domain.repository.PetEventRepository
import ru.ekrupin.ivi.domain.repository.PetRepository
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import ru.ekrupin.ivi.domain.repository.WeightRepository
import java.time.LocalDate

data class HomeHighlight(
    val title: String = "Пока ничего срочного",
    val subtitle: String = "Можно спокойно заглянуть в историю событий",
)

data class HomeEventItem(
    val title: String,
    val subtitle: String,
)

data class HomeConflictBanner(
    val count: Int,
)

data class HomeUiState(
    val petName: String = "",
    val birthDate: LocalDate? = null,
    val birthDateLabel: String = "",
    val ageLabel: String = "Возраст можно добавить",
    val photoUri: String? = null,
    val currentWeightLabel: String = "Пока нет записей",
    val nextHighlight: HomeHighlight = HomeHighlight(),
    val activeEvents: List<HomeEventItem> = emptyList(),
    val conflictBanner: HomeConflictBanner? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val weightRepository: WeightRepository,
    private val petEventRepository: PetEventRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val syncConflictRepository: SyncConflictRepository,
) : ViewModel() {
    private val baseUiState = combine(
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
                val suffix = event.dueDate?.let { "Контроль до ${it.toDisplayDate()}" } ?: "Проведено ${event.eventDate.toDisplayDate()}"
                HomeEventItem(title = title, subtitle = suffix)
            }

        val nextHighlight = settings?.let { reminder ->
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
                HomeHighlight(
                    title = title,
                    subtitle = "Напомним ${reminderDate.toDisplayDate()}",
                )
            }
        } ?: activeEvents.firstOrNull()?.let {
            HomeHighlight(
                title = it.title,
                subtitle = it.subtitle,
            )
        } ?: HomeHighlight()

        HomeUiState(
            petName = pet?.name ?: "Иви",
            birthDate = pet?.birthDate,
            birthDateLabel = pet?.birthDate?.toDisplayDate()?.let { "Дата рождения: $it" } ?: "Дату рождения можно добавить",
            ageLabel = pet?.birthDate?.toAgeLabel() ?: "Возраст можно добавить",
            photoUri = pet?.photoUri,
            currentWeightLabel = currentWeight,
            nextHighlight = nextHighlight,
            activeEvents = activeEvents.take(3),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseUiState,
        syncConflictRepository.observeConflictCount(),
    ) { baseUiState, conflictCount ->
        baseUiState.copy(
            conflictBanner = conflictCount.takeIf { it > 0 }?.let { count ->
                HomeConflictBanner(count = count)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun savePet(name: String, birthDate: LocalDate?, photoUri: String?) {
        viewModelScope.launch {
            petRepository.savePet(name = name, birthDate = birthDate, photoUri = photoUri)
        }
    }

    fun savePetPhoto(photoUri: String?) {
        viewModelScope.launch {
            petRepository.savePet(
                name = uiState.value.petName.ifBlank { "Иви" },
                birthDate = uiState.value.birthDate,
                photoUri = photoUri,
            )
        }
    }
}
