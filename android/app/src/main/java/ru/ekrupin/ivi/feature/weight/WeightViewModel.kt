package ru.ekrupin.ivi.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.core.util.toWeightLabel
import ru.ekrupin.ivi.domain.model.WeightEntry
import ru.ekrupin.ivi.domain.repository.WeightRepository
import java.time.LocalDate

data class WeightUiState(
    val currentWeightLabel: String = "Пока нет записей",
    val history: List<WeightEntry> = emptyList(),
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
) : ViewModel() {
    val uiState: StateFlow<WeightUiState> = weightRepository.observeWeightHistory()
        .map { history ->
            WeightUiState(
                currentWeightLabel = history.firstOrNull()?.weightGrams?.toWeightLabel() ?: "Пока нет записей",
                history = history,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState())

    fun addWeight(date: LocalDate, weightGrams: Int, comment: String) {
        viewModelScope.launch {
            weightRepository.addWeightRecord(date, weightGrams, comment.takeIf { it.isNotBlank() })
        }
    }
}
