package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.mapper.toEntity
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.domain.model.PetEvent
import ru.ekrupin.ivi.domain.model.PetEventStatus
import ru.ekrupin.ivi.domain.repository.PetEventRepository

class LocalPetEventRepository @Inject constructor(
    private val petEventDao: PetEventDao,
    private val reminderScheduler: ReminderScheduler,
) : PetEventRepository {
    override fun observeEvents(): Flow<List<PetEvent>> = petEventDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeEvent(id: Long): Flow<PetEvent?> = petEventDao.observeById(id).map { it?.toDomain() }

    override suspend fun saveEvent(event: PetEvent): Long {
        val now = LocalDateTime.now()
        val id = petEventDao.insert(
            event.copy(
                createdAt = if (event.id == 0L) now else event.createdAt,
                updatedAt = now,
            ).toEntity(),
        )
        reminderScheduler.refreshAll()
        return if (event.id == 0L) id else event.id
    }

    override suspend fun updateStatus(id: Long, status: PetEventStatus) {
        petEventDao.updateStatus(id, status, LocalDateTime.now())
        reminderScheduler.refreshAll()
    }

    override suspend fun deleteEvent(id: Long) {
        petEventDao.deleteById(id)
        reminderScheduler.refreshAll()
    }
}
