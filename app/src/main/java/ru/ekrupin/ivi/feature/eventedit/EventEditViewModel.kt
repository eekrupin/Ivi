package ru.ekrupin.ivi.feature.eventedit

import androidx.lifecycle.SavedStateHandle
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
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.domain.model.EventType
import ru.ekrupin.ivi.domain.model.PetEvent
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import ru.ekrupin.ivi.domain.repository.PetEventRepository
import java.time.LocalDate
import java.time.LocalDateTime

data class EventEditUiState(
    val existingEvent: PetEvent? = null,
    val eventTypes: List<EventType> = emptyList(),
    val saveCompletedTick: Int = 0,
)

@HiltViewModel
class EventEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val petEventRepository: PetEventRepository,
    eventTypeRepository: EventTypeRepository,
) : ViewModel() {
    private val eventId: Long? = savedStateHandle.get<Long>("eventId")?.takeIf { it != -1L }
    private val saveTick = MutableStateFlow(0)

    val uiState: StateFlow<EventEditUiState> = combine(
        eventId?.let { petEventRepository.observeEvent(it) } ?: kotlinx.coroutines.flow.flowOf(null),
        eventTypeRepository.observeTypes(),
        saveTick,
    ) { event, types, tick ->
        EventEditUiState(existingEvent = event, eventTypes = types, saveCompletedTick = tick)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventEditUiState())

    fun saveEvent(
        selectedTypeId: Long,
        eventDate: LocalDate,
        dueDate: LocalDate?,
        comment: String,
        notificationsEnabled: Boolean,
        defaultDurationDays: Int?,
    ) {
        viewModelScope.launch {
            val current = uiState.value.existingEvent
            val finalDueDate = dueDate ?: defaultDurationDays?.let { eventDate.plusDays(it.toLong()) }
            petEventRepository.saveEvent(
                PetEvent(
                    id = current?.id ?: 0L,
                    petId = current?.petId ?: AppConstants.PET_ID,
                    eventTypeId = selectedTypeId,
                    eventDate = eventDate,
                    dueDate = finalDueDate,
                    comment = comment.takeIf { it.isNotBlank() },
                    notificationsEnabled = notificationsEnabled,
                    status = current?.status ?: PetEventStatus.ACTIVE,
                    createdAt = current?.createdAt ?: LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
            saveTick.update { it + 1 }
        }
    }
}
