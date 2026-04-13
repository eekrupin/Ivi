package ru.ekrupin.ivi.feature.petedit

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
import ru.ekrupin.ivi.domain.model.Pet
import ru.ekrupin.ivi.domain.repository.PetRepository
import java.time.LocalDate

data class PetEditUiState(
    val pet: Pet? = null,
    val saveCompletedTick: Int = 0,
)

@HiltViewModel
class PetEditViewModel @Inject constructor(
    private val petRepository: PetRepository,
) : ViewModel() {
    private val saveTick = MutableStateFlow(0)

    val uiState: StateFlow<PetEditUiState> = combine(
        petRepository.observePet(),
        saveTick,
    ) { pet, tick ->
        PetEditUiState(pet = pet, saveCompletedTick = tick)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetEditUiState())

    fun savePet(name: String, birthDate: LocalDate?, photoUri: String?) {
        viewModelScope.launch {
            petRepository.savePet(name = name, birthDate = birthDate, photoUri = photoUri)
            saveTick.update { it + 1 }
        }
    }
}
