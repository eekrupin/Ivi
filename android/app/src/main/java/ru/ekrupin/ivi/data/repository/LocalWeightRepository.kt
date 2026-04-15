package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.sync.SyncOutboxRecorder
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.WeightEntry
import ru.ekrupin.ivi.domain.repository.WeightRepository

class LocalWeightRepository @Inject constructor(
    private val database: IviDatabase,
    private val weightEntryDao: WeightEntryDao,
    private val syncOutboxRecorder: SyncOutboxRecorder,
) : WeightRepository {
    override fun observeWeightHistory(): Flow<List<WeightEntry>> =
        weightEntryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addWeightRecord(date: LocalDate, weightGrams: Int, comment: String?) {
        val now = LocalDateTime.now()
        val entity = WeightEntryEntity(
            petId = AppConstants.PET_ID,
            date = date,
            weightGrams = weightGrams,
            comment = comment?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
            remoteId = UUID.randomUUID().toString(),
            syncState = SyncState.PENDING_UPLOAD,
        )
        database.withTransaction {
            val id = weightEntryDao.insert(entity)
            syncOutboxRecorder.enqueueWeightEntryUpsert(entity.copy(id = id))
        }
    }
}
