package ru.ekrupin.ivi.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.domain.model.EventType

interface EventTypeRepository {
    fun observeTypes(): Flow<List<EventType>>

    fun observeActiveTypes(): Flow<List<EventType>>

    suspend fun saveType(eventType: EventType)
}
