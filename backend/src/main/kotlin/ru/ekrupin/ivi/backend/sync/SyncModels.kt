package ru.ekrupin.ivi.backend.sync

import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.auth.UserProfileResponse
import ru.ekrupin.ivi.backend.db.model.EventTypeRecord
import ru.ekrupin.ivi.backend.db.model.PetEventRecord
import ru.ekrupin.ivi.backend.db.model.PetMembershipRecord
import ru.ekrupin.ivi.backend.db.model.PetRecord
import ru.ekrupin.ivi.backend.db.model.UserRecord
import ru.ekrupin.ivi.backend.db.model.WeightEntryRecord
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Serializable
data class SyncBootstrapResponse(
    val cursor: String,
    val snapshot: SyncSnapshotResponse,
    val notes: List<String> = emptyList(),
)

@Serializable
data class SyncSnapshotResponse(
    val users: List<SyncUserProfileResponse>,
    val pets: List<SyncPetResponse>,
    val memberships: List<SyncMembershipResponse>,
    val eventTypes: List<SyncEventTypeResponse>,
    val petEvents: List<SyncPetEventResponse>,
    val weightEntries: List<SyncWeightEntryResponse>,
)

@Serializable
data class SyncUserProfileResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val email: String,
    val displayName: String,
    val createdAt: String,
)

@Serializable
data class SyncPetResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val name: String,
    val birthDate: String? = null,
    val photoRevision: String? = null,
    val createdAt: String,
)

@Serializable
data class SyncMembershipResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val petId: String,
    val userId: String,
    val role: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class SyncEventTypeResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val petId: String,
    val name: String,
    val category: String,
    val defaultDurationDays: Int? = null,
    val isActive: Boolean,
    val colorArgb: Int? = null,
    val iconKey: String? = null,
    val createdAt: String,
)

@Serializable
data class SyncPetEventResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val petId: String,
    val eventTypeId: String,
    val eventDate: String,
    val dueDate: String? = null,
    val comment: String? = null,
    val notificationsEnabled: Boolean,
    val status: String,
    val createdAt: String,
)

@Serializable
data class SyncWeightEntryResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val petId: String,
    val date: String,
    val weightGrams: Int,
    val comment: String? = null,
    val createdAt: String,
)

fun UserRecord.toSyncUserProfileResponse(): SyncUserProfileResponse = SyncUserProfileResponse(
    id = id.toString(),
    version = 1,
    updatedAt = updatedAt.toApiInstantString(),
    email = email,
    displayName = displayName,
    createdAt = createdAt.toApiInstantString(),
)

fun PetRecord.toSyncPetResponse(): SyncPetResponse = SyncPetResponse(
    id = id.toString(),
    version = version,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = deletedAt?.toApiInstantString(),
    name = name,
    birthDate = birthDate?.toString(),
    photoRevision = photoRevision,
    createdAt = createdAt.toApiInstantString(),
)

fun PetMembershipRecord.toSyncMembershipResponse(): SyncMembershipResponse = SyncMembershipResponse(
    id = id.toString(),
    version = 1,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = null,
    petId = petId.toString(),
    userId = userId.toString(),
    role = role.name,
    status = status.name,
    createdAt = createdAt.toApiInstantString(),
)

fun EventTypeRecord.toSyncEventTypeResponse(): SyncEventTypeResponse = SyncEventTypeResponse(
    id = id.toString(),
    version = version,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = deletedAt?.toApiInstantString(),
    petId = petId.toString(),
    name = name,
    category = category.name,
    defaultDurationDays = defaultDurationDays,
    isActive = isActive,
    colorArgb = colorArgb,
    iconKey = iconKey,
    createdAt = createdAt.toApiInstantString(),
)

fun PetEventRecord.toSyncPetEventResponse(): SyncPetEventResponse = SyncPetEventResponse(
    id = id.toString(),
    version = version,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = deletedAt?.toApiInstantString(),
    petId = petId.toString(),
    eventTypeId = eventTypeId.toString(),
    eventDate = eventDate.toString(),
    dueDate = dueDate?.toString(),
    comment = comment,
    notificationsEnabled = notificationsEnabled,
    status = status.name,
    createdAt = createdAt.toApiInstantString(),
)

fun WeightEntryRecord.toSyncWeightEntryResponse(): SyncWeightEntryResponse = SyncWeightEntryResponse(
    id = id.toString(),
    version = version,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = deletedAt?.toApiInstantString(),
    petId = petId.toString(),
    date = date.toString(),
    weightGrams = weightGrams,
    comment = comment,
    createdAt = createdAt.toApiInstantString(),
)

internal fun Instant.toApiInstantString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(atOffset(ZoneOffset.UTC))
