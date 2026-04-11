package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.domain.model.WeightEntry
import ru.ekrupin.ivi.domain.repository.WeightRepository

class LocalWeightRepository @Inject constructor(
    private val weightEntryDao: WeightEntryDao,
) : WeightRepository {
    override fun observeWeightHistory(): Flow<List<WeightEntry>> =
        weightEntryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addWeightRecord(date: LocalDate, weightGrams: Int, comment: String?) {
        weightEntryDao.insert(
            WeightEntryEntity(
                petId = AppConstants.PET_ID,
                date = date,
                weightGrams = weightGrams,
                comment = comment?.takeIf { it.isNotBlank() },
                createdAt = LocalDateTime.now(),
            ),
        )
    }
}
