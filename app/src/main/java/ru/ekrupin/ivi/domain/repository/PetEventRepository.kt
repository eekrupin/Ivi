package ru.ekrupin.ivi.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.domain.model.PetEvent
import ru.ekrupin.ivi.domain.model.PetEventStatus

interface PetEventRepository {
    fun observeEvents(): Flow<List<PetEvent>>

    fun observeEvent(id: Long): Flow<PetEvent?>

    suspend fun saveEvent(event: PetEvent): Long

    suspend fun updateStatus(id: Long, status: PetEventStatus)
}
