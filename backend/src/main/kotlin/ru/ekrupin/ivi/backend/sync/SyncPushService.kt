package ru.ekrupin.ivi.backend.sync

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.EventCategoryEntity
import ru.ekrupin.ivi.backend.db.model.PetEventStatusEntity
import ru.ekrupin.ivi.backend.db.repository.CreateEventTypeCommand
import ru.ekrupin.ivi.backend.db.repository.CreatePetEventCommand
import ru.ekrupin.ivi.backend.db.repository.CreateWeightEntryCommand
import ru.ekrupin.ivi.backend.db.repository.EventTypeRepository
import ru.ekrupin.ivi.backend.db.repository.PetEventRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UpdateEventTypeCommand
import ru.ekrupin.ivi.backend.db.repository.UpdatePetEventCommand
import ru.ekrupin.ivi.backend.db.repository.UpdateWeightEntryCommand
import ru.ekrupin.ivi.backend.db.repository.WeightEntryRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class SyncPushService(
    private val petRepository: PetRepository,
    private val petMembershipRepository: PetMembershipRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val petEventRepository: PetEventRepository,
    private val weightEntryRepository: WeightEntryRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun pushForUser(userId: UUID, request: SyncPushRequest): SyncPushResponse {
        if (request.deviceId.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "missing_device_id", "deviceId обязателен")
        }
        request.lastKnownCursor?.let { SyncCursorCodec.decode(it) }

        val membership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец для sync push не найден")

        val pet = petRepository.findById(membership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val orderedMutations = request.mutations.sortedWith(compareBy<SyncMutationEnvelope> { precedence(it.entityType) })
        val accepted = mutableListOf<AcceptedMutationResponse>()
        val conflicts = mutableListOf<ConflictDtoResponse>()

        orderedMutations.forEach { mutation ->
            when (mutation.entityType) {
                "EVENT_TYPE" -> handleEventTypeMutation(pet.id, mutation, accepted, conflicts)
                "PET_EVENT" -> handlePetEventMutation(pet.id, mutation, accepted, conflicts)
                "WEIGHT_ENTRY" -> handleWeightEntryMutation(pet.id, mutation, accepted, conflicts)
                else -> throw ApiException(HttpStatusCode.BadRequest, "unsupported_sync_entity", "В V1 sync/push поддерживает только EVENT_TYPE, PET_EVENT и WEIGHT_ENTRY")
            }
        }

        return SyncPushResponse(
            accepted = accepted,
            conflicts = conflicts,
            cursor = SyncCursorCodec.changesCursor(Instant.now()),
            requiresBootstrap = false,
        )
    }

    private fun handleEventTypeMutation(
        currentPetId: UUID,
        mutation: SyncMutationEnvelope,
        accepted: MutableList<AcceptedMutationResponse>,
        conflicts: MutableList<ConflictDtoResponse>,
    ) {
        val entityId = mutation.entityId.toUuidOrBadRequest("entityId")
        when (mutation.operation) {
            "UPSERT" -> {
                val payload = mutation.payload ?: throw ApiException(HttpStatusCode.BadRequest, "missing_mutation_payload", "Для EVENT_TYPE UPSERT обязателен payload")
                val model = json.decodeFromJsonElement<SyncEventTypeWriteModel>(payload)
                val petId = model.petId.toUuidOrBadRequest("payload.petId")
                if (petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }

                val existing = eventTypeRepository.findById(entityId)
                if (existing == null) {
                    val created = eventTypeRepository.create(
                        entityId = entityId,
                        command = CreateEventTypeCommand(
                            petId = currentPetId,
                            name = model.name,
                            category = EventCategoryEntity.valueOf(model.category),
                            defaultDurationDays = model.defaultDurationDays,
                            isActive = model.isActive,
                            colorArgb = model.colorArgb,
                            iconKey = model.iconKey,
                        ),
                    )
                    accepted += mutation.toAccepted(created.version)
                } else {
                    if (existing.petId != currentPetId) {
                        throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                    }
                    if (mutation.baseVersion != existing.version) {
                        conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncEventTypeResponse()))
                        return
                    }
                    val updated = eventTypeRepository.update(
                        id = entityId,
                        command = UpdateEventTypeCommand(
                            name = model.name,
                            category = EventCategoryEntity.valueOf(model.category),
                            defaultDurationDays = model.defaultDurationDays,
                            isActive = model.isActive,
                            colorArgb = model.colorArgb,
                            iconKey = model.iconKey,
                        ),
                    ) ?: error("Updated event type not found")
                    accepted += mutation.toAccepted(updated.version)
                }
            }

            "DELETE" -> {
                val existing = eventTypeRepository.findById(entityId)
                if (existing == null) {
                    conflicts += mutation.toConflict(serverVersion = 0, reason = "INVALID_REFERENCE")
                    return
                }
                if (existing.petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }
                if (mutation.baseVersion != existing.version) {
                    conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncEventTypeResponse()))
                    return
                }
                val deleted = eventTypeRepository.softDelete(entityId) ?: error("Deleted event type not found")
                accepted += mutation.toAccepted(deleted.version)
            }

            else -> throw ApiException(HttpStatusCode.BadRequest, "unsupported_sync_operation", "Поддерживаются только UPSERT и DELETE")
        }
    }

    private fun handlePetEventMutation(
        currentPetId: UUID,
        mutation: SyncMutationEnvelope,
        accepted: MutableList<AcceptedMutationResponse>,
        conflicts: MutableList<ConflictDtoResponse>,
    ) {
        val entityId = mutation.entityId.toUuidOrBadRequest("entityId")
        when (mutation.operation) {
            "UPSERT" -> {
                val payload = mutation.payload ?: throw ApiException(HttpStatusCode.BadRequest, "missing_mutation_payload", "Для PET_EVENT UPSERT обязателен payload")
                val model = json.decodeFromJsonElement<SyncPetEventWriteModel>(payload)
                val petId = model.petId.toUuidOrBadRequest("payload.petId")
                val eventTypeId = model.eventTypeId.toUuidOrBadRequest("payload.eventTypeId")
                if (petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }
                val referencedEventType = eventTypeRepository.findById(eventTypeId)
                if (referencedEventType == null || referencedEventType.petId != currentPetId || referencedEventType.deletedAt != null) {
                    conflicts += mutation.toConflict(serverVersion = referencedEventType?.version ?: 0, reason = "INVALID_REFERENCE")
                    return
                }

                val existing = petEventRepository.findById(entityId)
                if (existing == null) {
                    val created = petEventRepository.create(
                        entityId = entityId,
                        command = CreatePetEventCommand(
                            petId = currentPetId,
                            eventTypeId = eventTypeId,
                            eventDate = LocalDate.parse(model.eventDate),
                            dueDate = model.dueDate?.let(LocalDate::parse),
                            comment = model.comment,
                            notificationsEnabled = model.notificationsEnabled,
                            status = PetEventStatusEntity.valueOf(model.status),
                        ),
                    )
                    accepted += mutation.toAccepted(created.version)
                } else {
                    if (existing.petId != currentPetId) {
                        throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                    }
                    if (mutation.baseVersion != existing.version) {
                        conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncPetEventResponse()))
                        return
                    }
                    val updated = petEventRepository.update(
                        id = entityId,
                        command = UpdatePetEventCommand(
                            eventTypeId = eventTypeId,
                            eventDate = LocalDate.parse(model.eventDate),
                            dueDate = model.dueDate?.let(LocalDate::parse),
                            comment = model.comment,
                            notificationsEnabled = model.notificationsEnabled,
                            status = PetEventStatusEntity.valueOf(model.status),
                        ),
                    ) ?: error("Updated pet event not found")
                    accepted += mutation.toAccepted(updated.version)
                }
            }

            "DELETE" -> {
                val existing = petEventRepository.findById(entityId)
                if (existing == null) {
                    conflicts += mutation.toConflict(serverVersion = 0, reason = "INVALID_REFERENCE")
                    return
                }
                if (existing.petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }
                if (mutation.baseVersion != existing.version) {
                    conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncPetEventResponse()))
                    return
                }
                val deleted = petEventRepository.softDelete(entityId) ?: error("Deleted pet event not found")
                accepted += mutation.toAccepted(deleted.version)
            }

            else -> throw ApiException(HttpStatusCode.BadRequest, "unsupported_sync_operation", "Поддерживаются только UPSERT и DELETE")
        }
    }

    private fun handleWeightEntryMutation(
        currentPetId: UUID,
        mutation: SyncMutationEnvelope,
        accepted: MutableList<AcceptedMutationResponse>,
        conflicts: MutableList<ConflictDtoResponse>,
    ) {
        val entityId = mutation.entityId.toUuidOrBadRequest("entityId")
        when (mutation.operation) {
            "UPSERT" -> {
                val payload = mutation.payload ?: throw ApiException(HttpStatusCode.BadRequest, "missing_mutation_payload", "Для WEIGHT_ENTRY UPSERT обязателен payload")
                val model = json.decodeFromJsonElement<SyncWeightEntryWriteModel>(payload)
                val petId = model.petId.toUuidOrBadRequest("payload.petId")
                if (petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }

                val existing = weightEntryRepository.findById(entityId)
                if (existing == null) {
                    val created = weightEntryRepository.create(
                        entityId = entityId,
                        command = CreateWeightEntryCommand(
                            petId = currentPetId,
                            date = LocalDate.parse(model.date),
                            weightGrams = model.weightGrams,
                            comment = model.comment,
                        ),
                    )
                    accepted += mutation.toAccepted(created.version)
                } else {
                    if (existing.petId != currentPetId) {
                        throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                    }
                    if (mutation.baseVersion != existing.version) {
                        conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncWeightEntryResponse()))
                        return
                    }
                    val updated = weightEntryRepository.update(
                        id = entityId,
                        command = UpdateWeightEntryCommand(
                            date = LocalDate.parse(model.date),
                            weightGrams = model.weightGrams,
                            comment = model.comment,
                        ),
                    ) ?: error("Updated weight entry not found")
                    accepted += mutation.toAccepted(updated.version)
                }
            }

            "DELETE" -> {
                val existing = weightEntryRepository.findById(entityId)
                if (existing == null) {
                    conflicts += mutation.toConflict(serverVersion = 0, reason = "INVALID_REFERENCE")
                    return
                }
                if (existing.petId != currentPetId) {
                    throw ApiException(HttpStatusCode.Forbidden, "push_pet_forbidden", "Нельзя пушить изменения для чужого питомца")
                }
                if (mutation.baseVersion != existing.version) {
                    conflicts += mutation.toConflict(existing.version, "VERSION_MISMATCH", json.encodeToJsonElement(existing.toSyncWeightEntryResponse()))
                    return
                }
                val deleted = weightEntryRepository.softDelete(entityId) ?: error("Deleted weight entry not found")
                accepted += mutation.toAccepted(deleted.version)
            }

            else -> throw ApiException(HttpStatusCode.BadRequest, "unsupported_sync_operation", "Поддерживаются только UPSERT и DELETE")
        }
    }

    private fun precedence(entityType: String): Int = when (entityType) {
        "EVENT_TYPE" -> 0
        "PET_EVENT" -> 1
        "WEIGHT_ENTRY" -> 2
        else -> 100
    }

    private fun String.toUuidOrBadRequest(field: String): UUID = runCatching { UUID.fromString(this) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_uuid", "Поле $field должно быть UUID")
    }

    private fun SyncMutationEnvelope.toAccepted(version: Long): AcceptedMutationResponse = AcceptedMutationResponse(
        clientMutationId = clientMutationId,
        entityType = entityType,
        entityId = entityId,
        version = version,
    )

    private fun SyncMutationEnvelope.toConflict(
        serverVersion: Long,
        reason: String,
        serverRecord: kotlinx.serialization.json.JsonElement? = null,
    ): ConflictDtoResponse = ConflictDtoResponse(
        entityType = entityType,
        entityId = entityId,
        clientMutationId = clientMutationId,
        baseVersion = baseVersion,
        serverVersion = serverVersion,
        reason = reason,
        serverRecord = serverRecord,
    )
}
