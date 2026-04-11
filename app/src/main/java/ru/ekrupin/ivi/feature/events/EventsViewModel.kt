package ru.ekrupin.ivi.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.core.util.toDisplayDate
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import ru.ekrupin.ivi.domain.repository.PetEventRepository

data class EventListItemUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val status: PetEventStatus,
    val notificationsEnabled: Boolean,
)

data class EventsUiState(
    val filter: PetEventStatus = PetEventStatus.ACTIVE,
    val items: List<EventListItemUi> = emptyList(),
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val petEventRepository: PetEventRepository,
    eventTypeRepository: EventTypeRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(PetEventStatus.ACTIVE)

    val uiState: StateFlow<EventsUiState> = combine(
        petEventRepository.observeEvents(),
        eventTypeRepository.observeTypes(),
        filter,
    ) { events, types, currentFilter ->
        val typeNames = types.associateBy({ it.id }, { it.name })
        val items = events
            .filter { it.status == currentFilter }
            .map { event ->
                val title = typeNames[event.eventTypeId] ?: "Событие"
                val subtitle = buildString {
                    append("Дата ${event.eventDate.toDisplayDate()}")
                    event.dueDate?.let {
                        append(", контроль ${it.toDisplayDate()}")
                    }
                    event.comment?.takeIf { it.isNotBlank() }?.let {
                        append("\n$it")
                    }
                }
                EventListItemUi(
                    id = event.id,
                    title = title,
                    subtitle = subtitle,
                    status = event.status,
                    notificationsEnabled = event.notificationsEnabled,
                )
            }
        EventsUiState(filter = currentFilter, items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    fun setFilter(status: PetEventStatus) {
        filter.update { status }
    }

    fun updateStatus(id: Long, status: PetEventStatus) {
        viewModelScope.launch {
            petEventRepository.updateStatus(id, status)
        }
    }
}
