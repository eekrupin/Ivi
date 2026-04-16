package ru.ekrupin.ivi.feature.syncconflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictListItem
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictRepository

@HiltViewModel
class SyncConflictsViewModel @Inject constructor(
    private val syncConflictRepository: SyncConflictRepository,
) : ViewModel() {
    val conflicts: StateFlow<List<SyncConflictListItem>> = syncConflictRepository.observeConflicts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun acceptServerVersion(conflictId: Long) {
        viewModelScope.launch {
            syncConflictRepository.acceptServerVersion(conflictId)
        }
    }

    fun retryLocalChanges(conflictId: Long) {
        viewModelScope.launch {
            syncConflictRepository.retryLocalChanges(conflictId)
        }
    }
}
