package ru.ekrupin.ivi.feature.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.EventType
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import java.time.LocalDateTime

@HiltViewModel
class EventTypesViewModel @Inject constructor(
    private val eventTypeRepository: EventTypeRepository,
) : ViewModel() {
    val types: StateFlow<List<EventType>> = eventTypeRepository.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveType(
        id: Long,
        name: String,
        category: EventCategory,
        defaultDurationDays: Int?,
        isActive: Boolean,
    ) {
        viewModelScope.launch {
            eventTypeRepository.saveType(
                EventType(
                    id = id,
                    name = name,
                    category = category,
                    defaultDurationDays = defaultDurationDays,
                    isActive = isActive,
                    colorArgb = null,
                    iconKey = null,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }
}
