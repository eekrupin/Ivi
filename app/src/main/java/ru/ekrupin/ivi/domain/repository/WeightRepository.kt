package ru.ekrupin.ivi.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.domain.model.WeightEntry

interface WeightRepository {
    fun observeWeightHistory(): Flow<List<WeightEntry>>

    suspend fun addWeightRecord(
        date: java.time.LocalDate,
        weightGrams: Int,
        comment: String?,
    )
}
