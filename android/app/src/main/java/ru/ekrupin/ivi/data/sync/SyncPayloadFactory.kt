package ru.ekrupin.ivi.data.sync

import java.util.UUID
import org.json.JSONObject
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import java.time.LocalDateTime

class SyncPayloadFactory {
    fun eventTypeUpsert(
        entity: EventTypeEntity,
        pet: PetEntity,
        now: LocalDateTime,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.EVENT_TYPE,
        entityLocalId = entity.id,
        entityRemoteId = entity.remoteId.orEmpty(),
        operation = SyncOperation.UPSERT,
        payloadJson = JSONObject().apply {
            put("petId", pet.requireRemoteId())
            put("name", entity.name)
            put("category", entity.category.name)
            put("defaultDurationDays", entity.defaultDurationDays)
            put("isActive", entity.isActive)
            put("colorArgb", entity.colorArgb)
            put("iconKey", entity.iconKey)
        }.toString(),
        baseVersion = entity.serverVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun eventTypeDelete(entity: EventTypeEntity, now: LocalDateTime): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.EVENT_TYPE,
        entityLocalId = entity.id,
        entityRemoteId = entity.remoteId.orEmpty(),
        operation = SyncOperation.DELETE,
        payloadJson = null,
        baseVersion = entity.serverVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun petEventUpsert(
        entity: PetEventEntity,
        pet: PetEntity,
        eventType: EventTypeEntity,
        now: LocalDateTime,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.PET_EVENT,
        entityLocalId = entity.id,
        entityRemoteId = entity.remoteId.orEmpty(),
        operation = SyncOperation.UPSERT,
        payloadJson = JSONObject().apply {
            put("petId", pet.requireRemoteId())
            put("eventTypeId", eventType.requireRemoteId())
            put("eventDate", entity.eventDate.toString())
            put("dueDate", entity.dueDate?.toString())
            put("comment", entity.comment)
            put("notificationsEnabled", entity.notificationsEnabled)
            put("status", entity.status.name)
        }.toString(),
        baseVersion = entity.serverVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun petEventDelete(entity: PetEventEntity, now: LocalDateTime): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.PET_EVENT,
        entityLocalId = entity.id,
        entityRemoteId = entity.remoteId.orEmpty(),
        operation = SyncOperation.DELETE,
        payloadJson = null,
        baseVersion = entity.serverVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun weightEntryUpsert(
        entity: WeightEntryEntity,
        pet: PetEntity,
        now: LocalDateTime,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.WEIGHT_ENTRY,
        entityLocalId = entity.id,
        entityRemoteId = entity.remoteId.orEmpty(),
        operation = SyncOperation.UPSERT,
        payloadJson = JSONObject().apply {
            put("petId", pet.requireRemoteId())
            put("date", entity.date.toString())
            put("weightGrams", entity.weightGrams)
            put("comment", entity.comment)
        }.toString(),
        baseVersion = entity.serverVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    private fun PetEntity.requireRemoteId(): String = remoteId ?: error("Pet remoteId is missing")
    private fun EventTypeEntity.requireRemoteId(): String = remoteId ?: error("EventType remoteId is missing")
}
