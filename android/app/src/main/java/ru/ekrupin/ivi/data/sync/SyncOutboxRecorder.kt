package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity

class SyncOutboxRecorder @Inject constructor(
    private val petDao: PetDao,
    private val eventTypeDao: EventTypeDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val payloadFactory: SyncPayloadFactory,
) {
    suspend fun enqueueEventTypeUpsert(entity: EventTypeEntity) {
        val pet = petDao.getPet() ?: error("Pet is missing for event type outbox")
        syncOutboxDao.insert(payloadFactory.eventTypeUpsert(entity, pet, LocalDateTime.now()))
    }

    suspend fun enqueueEventTypeDelete(entity: EventTypeEntity) {
        syncOutboxDao.insert(payloadFactory.eventTypeDelete(entity, LocalDateTime.now()))
    }

    suspend fun enqueuePetEventUpsert(entity: PetEventEntity) {
        val pet = petDao.getPet() ?: error("Pet is missing for pet event outbox")
        val eventType = eventTypeDao.getById(entity.eventTypeId) ?: error("Event type is missing for pet event outbox")
        syncOutboxDao.insert(payloadFactory.petEventUpsert(entity, pet, eventType, LocalDateTime.now()))
    }

    suspend fun enqueuePetEventDelete(entity: PetEventEntity) {
        syncOutboxDao.insert(payloadFactory.petEventDelete(entity, LocalDateTime.now()))
    }

    suspend fun enqueueWeightEntryUpsert(entity: WeightEntryEntity) {
        val pet = petDao.getPet() ?: error("Pet is missing for weight outbox")
        syncOutboxDao.insert(payloadFactory.weightEntryUpsert(entity, pet, LocalDateTime.now()))
    }
}
