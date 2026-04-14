package ru.ekrupin.ivi.backend.domain

import ru.ekrupin.ivi.backend.db.model.EventTypeRecord
import ru.ekrupin.ivi.backend.db.model.PetEventRecord
import ru.ekrupin.ivi.backend.db.model.WeightEntryRecord
import ru.ekrupin.ivi.backend.db.repository.CreateEventTypeCommand
import ru.ekrupin.ivi.backend.db.repository.CreatePetEventCommand
import ru.ekrupin.ivi.backend.db.repository.CreateWeightEntryCommand
import ru.ekrupin.ivi.backend.db.repository.EventTypeRepository
import ru.ekrupin.ivi.backend.db.repository.PetEventRepository
import ru.ekrupin.ivi.backend.db.repository.UpdateEventTypeCommand
import ru.ekrupin.ivi.backend.db.repository.UpdatePetEventCommand
import ru.ekrupin.ivi.backend.db.repository.UpdateWeightEntryCommand
import ru.ekrupin.ivi.backend.db.repository.WeightEntryRepository
import java.time.Instant
import java.util.UUID

class PetDomainDataService(
    private val eventTypeRepository: EventTypeRepository,
    private val petEventRepository: PetEventRepository,
    private val weightEntryRepository: WeightEntryRepository,
) {
    fun listEventTypes(petId: UUID, includeDeleted: Boolean = false): List<EventTypeRecord> =
        eventTypeRepository.listByPetId(petId, includeDeleted)

    fun createEventType(command: CreateEventTypeCommand): EventTypeRecord = eventTypeRepository.create(command)

    fun updateEventType(id: UUID, command: UpdateEventTypeCommand): EventTypeRecord? = eventTypeRepository.update(id, command)

    fun deleteEventType(id: UUID): EventTypeRecord? = eventTypeRepository.softDelete(id)

    fun listChangedEventTypes(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<EventTypeRecord> =
        eventTypeRepository.listChangedByPetId(petId, sinceExclusive, untilInclusive)

    fun listPetEvents(petId: UUID, includeDeleted: Boolean = false): List<PetEventRecord> =
        petEventRepository.listByPetId(petId, includeDeleted)

    fun createPetEvent(command: CreatePetEventCommand): PetEventRecord = petEventRepository.create(command)

    fun updatePetEvent(id: UUID, command: UpdatePetEventCommand): PetEventRecord? = petEventRepository.update(id, command)

    fun deletePetEvent(id: UUID): PetEventRecord? = petEventRepository.softDelete(id)

    fun listChangedPetEvents(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<PetEventRecord> =
        petEventRepository.listChangedByPetId(petId, sinceExclusive, untilInclusive)

    fun listWeightEntries(petId: UUID, includeDeleted: Boolean = false): List<WeightEntryRecord> =
        weightEntryRepository.listByPetId(petId, includeDeleted)

    fun createWeightEntry(command: CreateWeightEntryCommand): WeightEntryRecord = weightEntryRepository.create(command)

    fun updateWeightEntry(id: UUID, command: UpdateWeightEntryCommand): WeightEntryRecord? = weightEntryRepository.update(id, command)

    fun deleteWeightEntry(id: UUID): WeightEntryRecord? = weightEntryRepository.softDelete(id)

    fun listChangedWeightEntries(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<WeightEntryRecord> =
        weightEntryRepository.listChangedByPetId(petId, sinceExclusive, untilInclusive)
}
