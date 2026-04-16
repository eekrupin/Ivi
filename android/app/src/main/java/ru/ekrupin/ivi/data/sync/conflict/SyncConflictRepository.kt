package ru.ekrupin.ivi.data.sync.conflict

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.SyncConflictDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.SyncConflictEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.data.sync.SyncPayloadFactory
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus

data class SyncConflictListItem(
    val id: Long,
    val entityType: SyncEntityType,
    val title: String,
    val subtitle: String,
    val reasonText: String,
    val conflictedAt: LocalDateTime,
    val hasServerSnapshot: Boolean,
)

class SyncConflictRepository @Inject constructor(
    private val database: IviDatabase,
    private val syncConflictDao: SyncConflictDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val petDao: PetDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val weightEntryDao: WeightEntryDao,
    private val payloadFactory: SyncPayloadFactory,
    private val reminderScheduler: ReminderScheduler,
) {
    fun observeConflicts(): Flow<List<SyncConflictListItem>> =
        syncConflictDao.observeAll().map { items ->
            items.mapNotNull { conflict -> conflict.toListItem() }
        }

    fun observeConflictCount(): Flow<Int> = syncConflictDao.observeAll().map { it.size }

    suspend fun acceptServerVersion(conflictId: Long) {
        val conflict = syncConflictDao.getById(conflictId) ?: return
        val now = LocalDateTime.now()
        database.withTransaction {
            when (conflict.entityType) {
                SyncEntityType.EVENT_TYPE -> acceptEventType(conflict, now)
                SyncEntityType.PET_EVENT -> acceptPetEvent(conflict, now)
                SyncEntityType.WEIGHT_ENTRY -> acceptWeightEntry(conflict, now)
                else -> error("Unsupported conflict entity type: ${conflict.entityType}")
            }
            cleanupConflict(conflict)
        }
        if (conflict.entityType == SyncEntityType.PET_EVENT) {
            reminderScheduler.refreshAll()
        }
    }

    suspend fun retryLocalChanges(conflictId: Long) {
        val conflict = syncConflictDao.getById(conflictId) ?: return
        val now = LocalDateTime.now()
        database.withTransaction {
            syncOutboxDao.deleteByEntity(conflict.entityType, conflict.entityLocalId)
            when (conflict.entityType) {
                SyncEntityType.EVENT_TYPE -> retryEventType(conflict, now)
                SyncEntityType.PET_EVENT -> retryPetEvent(conflict, now)
                SyncEntityType.WEIGHT_ENTRY -> retryWeightEntry(conflict, now)
                else -> error("Unsupported conflict entity type: ${conflict.entityType}")
            }
            syncConflictDao.deleteById(conflict.id)
        }
        if (conflict.entityType == SyncEntityType.PET_EVENT) {
            reminderScheduler.refreshAll()
        }
    }

    private suspend fun acceptEventType(conflict: SyncConflictEntity, now: LocalDateTime) {
        val existing = eventTypeDao.getById(conflict.entityLocalId)
        val snapshot = conflict.serverRecordJson?.let(::parseEventType)
        if (snapshot != null) {
            eventTypeDao.insert(
                EventTypeEntity(
                    id = existing?.id ?: 0,
                    name = snapshot.name,
                    category = snapshot.category,
                    defaultDurationDays = snapshot.defaultDurationDays,
                    isActive = snapshot.isActive,
                    colorArgb = snapshot.colorArgb,
                    iconKey = snapshot.iconKey,
                    createdAt = snapshot.createdAt,
                    updatedAt = snapshot.serverUpdatedAt,
                    remoteId = snapshot.remoteId,
                    serverVersion = snapshot.serverVersion,
                    serverUpdatedAt = snapshot.serverUpdatedAt,
                    deletedAt = snapshot.deletedAt,
                    syncState = SyncState.SYNCED,
                    lastSyncedAt = now,
                ),
            )
        } else if (existing != null) {
            eventTypeDao.insert(
                existing.copy(
                    isActive = false,
                    deletedAt = existing.deletedAt ?: now,
                    updatedAt = now,
                    serverVersion = conflict.serverVersion.takeIf { it > 0 } ?: existing.serverVersion,
                    serverUpdatedAt = now,
                    syncState = SyncState.SYNCED,
                    lastSyncedAt = now,
                ),
            )
        }
    }

    private suspend fun acceptPetEvent(conflict: SyncConflictEntity, now: LocalDateTime) {
        val existing = petEventDao.getById(conflict.entityLocalId)
        val snapshot = conflict.serverRecordJson?.let(::parsePetEvent)
        if (snapshot != null) {
            val eventType = eventTypeDao.getByRemoteId(snapshot.eventTypeRemoteId)
            if (eventType != null) {
                petEventDao.insert(
                    PetEventEntity(
                        id = existing?.id ?: 0,
                        petId = AppConstants.PET_ID,
                        eventTypeId = eventType.id,
                        eventDate = snapshot.eventDate,
                        dueDate = snapshot.dueDate,
                        comment = snapshot.comment,
                        notificationsEnabled = snapshot.notificationsEnabled,
                        status = snapshot.status,
                        createdAt = snapshot.createdAt,
                        updatedAt = snapshot.serverUpdatedAt,
                        remoteId = snapshot.remoteId,
                        serverVersion = snapshot.serverVersion,
                        serverUpdatedAt = snapshot.serverUpdatedAt,
                        deletedAt = snapshot.deletedAt,
                        syncState = SyncState.SYNCED,
                        lastSyncedAt = now,
                    ),
                )
                return
            }
        }
        if (existing != null) {
            petEventDao.insert(
                existing.copy(
                    deletedAt = existing.deletedAt ?: now,
                    updatedAt = now,
                    serverVersion = conflict.serverVersion.takeIf { it > 0 } ?: existing.serverVersion,
                    serverUpdatedAt = now,
                    syncState = SyncState.SYNCED,
                    lastSyncedAt = now,
                ),
            )
        }
    }

    private suspend fun acceptWeightEntry(conflict: SyncConflictEntity, now: LocalDateTime) {
        val existing = weightEntryDao.getById(conflict.entityLocalId)
        val snapshot = conflict.serverRecordJson?.let(::parseWeightEntry)
        if (snapshot != null) {
            weightEntryDao.insert(
                WeightEntryEntity(
                    id = existing?.id ?: 0,
                    petId = AppConstants.PET_ID,
                    date = snapshot.date,
                    weightGrams = snapshot.weightGrams,
                    comment = snapshot.comment,
                    createdAt = snapshot.createdAt,
                    updatedAt = snapshot.serverUpdatedAt,
                    remoteId = snapshot.remoteId,
                    serverVersion = snapshot.serverVersion,
                    serverUpdatedAt = snapshot.serverUpdatedAt,
                    deletedAt = snapshot.deletedAt,
                    syncState = SyncState.SYNCED,
                    lastSyncedAt = now,
                ),
            )
        } else if (existing != null) {
            weightEntryDao.insert(
                existing.copy(
                    deletedAt = existing.deletedAt ?: now,
                    updatedAt = now,
                    serverVersion = conflict.serverVersion.takeIf { it > 0 } ?: existing.serverVersion,
                    serverUpdatedAt = now,
                    syncState = SyncState.SYNCED,
                    lastSyncedAt = now,
                ),
            )
        }
    }

    private suspend fun retryEventType(conflict: SyncConflictEntity, now: LocalDateTime) {
        val entity = eventTypeDao.getById(conflict.entityLocalId) ?: return
        val rebased = entity.copy(
            serverVersion = normalizeBaseVersion(conflict.serverVersion),
            syncState = SyncState.PENDING_UPLOAD,
            lastSyncedAt = now,
        )
        eventTypeDao.insert(rebased)
        val pet = petDao.getPet() ?: error("Pet is missing for conflict retry")
        val outbox = if (rebased.deletedAt != null) {
            payloadFactory.eventTypeDelete(rebased, now, rebased.serverVersion)
        } else {
            payloadFactory.eventTypeUpsert(rebased, pet, now, rebased.serverVersion)
        }
        syncOutboxDao.insert(outbox)
    }

    private suspend fun retryPetEvent(conflict: SyncConflictEntity, now: LocalDateTime) {
        val entity = petEventDao.getById(conflict.entityLocalId) ?: return
        val rebased = entity.copy(
            serverVersion = normalizeBaseVersion(conflict.serverVersion),
            syncState = SyncState.PENDING_UPLOAD,
            lastSyncedAt = now,
        )
        petEventDao.insert(rebased)
        val outbox = if (rebased.deletedAt != null) {
            payloadFactory.petEventDelete(rebased, now, rebased.serverVersion)
        } else {
            val pet = petDao.getPet() ?: error("Pet is missing for conflict retry")
            val eventType = eventTypeDao.getById(rebased.eventTypeId) ?: error("Event type is missing for conflict retry")
            payloadFactory.petEventUpsert(rebased, pet, eventType, now, rebased.serverVersion)
        }
        syncOutboxDao.insert(outbox)
    }

    private suspend fun retryWeightEntry(conflict: SyncConflictEntity, now: LocalDateTime) {
        val entity = weightEntryDao.getById(conflict.entityLocalId) ?: return
        val rebased = entity.copy(
            serverVersion = normalizeBaseVersion(conflict.serverVersion),
            syncState = SyncState.PENDING_UPLOAD,
            lastSyncedAt = now,
        )
        weightEntryDao.insert(rebased)
        val pet = petDao.getPet() ?: error("Pet is missing for conflict retry")
        syncOutboxDao.insert(payloadFactory.weightEntryUpsert(rebased, pet, now, rebased.serverVersion))
    }

    private suspend fun cleanupConflict(conflict: SyncConflictEntity) {
        syncOutboxDao.deleteByEntity(conflict.entityType, conflict.entityLocalId)
        syncConflictDao.deleteById(conflict.id)
    }

    private suspend fun SyncConflictEntity.toListItem(): SyncConflictListItem? = when (entityType) {
        SyncEntityType.EVENT_TYPE -> {
            val entity = eventTypeDao.getById(entityLocalId)
            SyncConflictListItem(
                id = id,
                entityType = entityType,
                title = entity?.name ?: "Тип события #$entityLocalId",
                subtitle = entity?.category?.toLabel() ?: "Локальная запись больше не найдена",
                reasonText = reason.toConflictText(),
                conflictedAt = conflictedAt,
                hasServerSnapshot = serverRecordJson != null,
            )
        }

        SyncEntityType.PET_EVENT -> {
            val entity = petEventDao.getById(entityLocalId)
            val eventTypeName = entity?.let { eventTypeDao.getById(it.eventTypeId)?.name }
            SyncConflictListItem(
                id = id,
                entityType = entityType,
                title = eventTypeName ?: "Событие #$entityLocalId",
                subtitle = entity?.eventDate?.toDisplayDate() ?: "Локальная запись больше не найдена",
                reasonText = reason.toConflictText(),
                conflictedAt = conflictedAt,
                hasServerSnapshot = serverRecordJson != null,
            )
        }

        SyncEntityType.WEIGHT_ENTRY -> {
            val entity = weightEntryDao.getById(entityLocalId)
            SyncConflictListItem(
                id = id,
                entityType = entityType,
                title = entity?.date?.toDisplayDate()?.let { "Вес от $it" } ?: "Запись веса #$entityLocalId",
                subtitle = entity?.let { formatWeight(it.weightGrams) } ?: "Локальная запись больше не найдена",
                reasonText = reason.toConflictText(),
                conflictedAt = conflictedAt,
                hasServerSnapshot = serverRecordJson != null,
            )
        }

        else -> null
    }

    private fun parseEventType(json: String): ParsedEventType {
        val item = JSONObject(json)
        return ParsedEventType(
            remoteId = item.getString("id"),
            name = item.getString("name"),
            category = EventCategory.valueOf(item.getString("category")),
            defaultDurationDays = item.optIntOrNull("defaultDurationDays"),
            isActive = item.getBoolean("isActive"),
            colorArgb = item.optLongOrNull("colorArgb"),
            iconKey = item.optStringOrNull("iconKey"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun parsePetEvent(json: String): ParsedPetEvent {
        val item = JSONObject(json)
        return ParsedPetEvent(
            remoteId = item.getString("id"),
            eventTypeRemoteId = item.getString("eventTypeId"),
            eventDate = LocalDate.parse(item.getString("eventDate")),
            dueDate = item.optStringOrNull("dueDate")?.let(LocalDate::parse),
            comment = item.optStringOrNull("comment"),
            notificationsEnabled = item.getBoolean("notificationsEnabled"),
            status = PetEventStatus.valueOf(item.getString("status")),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun parseWeightEntry(json: String): ParsedWeightEntry {
        val item = JSONObject(json)
        return ParsedWeightEntry(
            remoteId = item.getString("id"),
            date = LocalDate.parse(item.getString("date")),
            weightGrams = item.getInt("weightGrams"),
            comment = item.optStringOrNull("comment"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun normalizeBaseVersion(serverVersion: Long): Long? = serverVersion.takeIf { it > 0 }

    private fun EventCategory.toLabel(): String = when (this) {
        EventCategory.TICK_PROTECTION -> "От клещей"
        EventCategory.DEWORMING -> "От глистов"
        EventCategory.VACCINATION -> "Прививка"
        EventCategory.CHECKUP -> "Осмотр"
        EventCategory.OTHER -> "Другое"
    }

    private fun String.toConflictText(): String = when (this) {
        "VERSION_MISMATCH" -> "Локальная версия расходится с серверной"
        "INVALID_REFERENCE" -> "Сервер не принял запись из-за связанной сущности"
        else -> "Запись конфликтует с серверной версией"
    }

    private fun LocalDate.toDisplayDate(): String = format(date = this)

    private fun formatWeight(weightGrams: Int): String = String.format(java.util.Locale.US, "%.1f кг", weightGrams / 1000.0)

    private fun format(date: LocalDate): String = date.format(DATE_FORMATTER)

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) getInt(name) else null

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (has(name) && !isNull(name)) getLong(name) else null

    private fun String.toLocalDateTimeUtc(): LocalDateTime = OffsetDateTime.parse(this).toLocalDateTime()

    private data class ParsedEventType(
        val remoteId: String,
        val name: String,
        val category: EventCategory,
        val defaultDurationDays: Int?,
        val isActive: Boolean,
        val colorArgb: Long?,
        val iconKey: String?,
        val serverVersion: Long,
        val serverUpdatedAt: LocalDateTime,
        val deletedAt: LocalDateTime?,
        val createdAt: LocalDateTime,
    )

    private data class ParsedPetEvent(
        val remoteId: String,
        val eventTypeRemoteId: String,
        val eventDate: LocalDate,
        val dueDate: LocalDate?,
        val comment: String?,
        val notificationsEnabled: Boolean,
        val status: PetEventStatus,
        val serverVersion: Long,
        val serverUpdatedAt: LocalDateTime,
        val deletedAt: LocalDateTime?,
        val createdAt: LocalDateTime,
    )

    private data class ParsedWeightEntry(
        val remoteId: String,
        val date: LocalDate,
        val weightGrams: Int,
        val comment: String?,
        val serverVersion: Long,
        val serverUpdatedAt: LocalDateTime,
        val deletedAt: LocalDateTime?,
        val createdAt: LocalDateTime,
    )

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
