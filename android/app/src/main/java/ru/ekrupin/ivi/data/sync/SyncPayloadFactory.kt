package ru.ekrupin.ivi.data.sync

import java.time.LocalDateTime
import java.util.UUID
import org.json.JSONObject
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus

class SyncPayloadFactory {
    fun eventTypeUpsert(
        entity: EventTypeEntity,
        pet: PetEntity,
        now: LocalDateTime,
        baseVersion: Long? = entity.serverVersion,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.EVENT_TYPE,
        entityLocalId = entity.id,
        entityRemoteId = entity.requireRemoteId(),
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
        baseVersion = baseVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun eventTypeDelete(entity: EventTypeEntity, now: LocalDateTime, baseVersion: Long? = entity.serverVersion): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.EVENT_TYPE,
        entityLocalId = entity.id,
        entityRemoteId = entity.requireRemoteId(),
        operation = SyncOperation.DELETE,
        payloadJson = null,
        baseVersion = baseVersion,
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
        baseVersion: Long? = entity.serverVersion,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.PET_EVENT,
        entityLocalId = entity.id,
        entityRemoteId = entity.requireRemoteId(),
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
        baseVersion = baseVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun petEventDelete(entity: PetEventEntity, now: LocalDateTime, baseVersion: Long? = entity.serverVersion): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.PET_EVENT,
        entityLocalId = entity.id,
        entityRemoteId = entity.requireRemoteId(),
        operation = SyncOperation.DELETE,
        payloadJson = null,
        baseVersion = baseVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    fun weightEntryUpsert(
        entity: WeightEntryEntity,
        pet: PetEntity,
        now: LocalDateTime,
        baseVersion: Long? = entity.serverVersion,
    ): SyncOutboxEntity = SyncOutboxEntity(
        entityType = SyncEntityType.WEIGHT_ENTRY,
        entityLocalId = entity.id,
        entityRemoteId = entity.requireRemoteId(),
        operation = SyncOperation.UPSERT,
        payloadJson = JSONObject().apply {
            put("petId", pet.requireRemoteId())
            put("date", entity.date.toString())
            put("weightGrams", entity.weightGrams)
            put("comment", entity.comment)
        }.toString(),
        baseVersion = baseVersion,
        clientMutationId = UUID.randomUUID().toString(),
        status = SyncOutboxStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )

    private fun PetEntity.requireRemoteId(): String = remoteId.requireRemoteId("Pet", id)
    private fun EventTypeEntity.requireRemoteId(): String = remoteId.requireRemoteId("EventType", id)
    private fun PetEventEntity.requireRemoteId(): String = remoteId.requireRemoteId("PetEvent", id)
    private fun WeightEntryEntity.requireRemoteId(): String = remoteId.requireRemoteId("WeightEntry", id)

    private fun String?.requireRemoteId(entityType: String, localId: Long): String {
        val value = this?.takeIf { it.isNotBlank() }
            ?: error("$entityType remoteId is missing for local id $localId")
        val isValidUuid = runCatching { UUID.fromString(value) }
            .getOrNull()
            ?.toString()
            ?.equals(value, ignoreCase = true) == true
        check(isValidUuid) { "$entityType remoteId is not a UUID for local id $localId" }
        return value
    }
}
