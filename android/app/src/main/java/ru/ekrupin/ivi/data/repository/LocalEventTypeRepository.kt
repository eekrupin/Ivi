package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.mapper.toEntity
import ru.ekrupin.ivi.domain.model.EventType
import ru.ekrupin.ivi.domain.repository.EventTypeRepository

class LocalEventTypeRepository @Inject constructor(
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
) : EventTypeRepository {
    override fun observeTypes(): Flow<List<EventType>> = eventTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveTypes(): Flow<List<EventType>> = eventTypeDao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun saveType(eventType: EventType) {
        val now = LocalDateTime.now()
        val existing = eventType.id.takeIf { it != 0L }?.let { eventTypeDao.getById(it) }
        val entity = eventType.copy(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        ).toEntity()

        if (existing == null) {
            eventTypeDao.insert(entity)
        } else {
            eventTypeDao.update(entity)
        }
    }

    override suspend fun deleteType(id: Long) {
        if (petEventDao.countByEventTypeId(id) == 0) {
            eventTypeDao.deleteById(id)
            return
        }

        val existing = eventTypeDao.getById(id) ?: return
        eventTypeDao.update(
            existing.copy(
                isActive = false,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }
}
