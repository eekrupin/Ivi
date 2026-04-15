package ru.ekrupin.ivi.data.sync.remote

import java.time.LocalDate
import java.time.LocalDateTime

data class RemoteSyncUser(
    val remoteId: String,
    val email: String,
    val displayName: String,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteSyncPet(
    val remoteId: String,
    val name: String,
    val birthDate: LocalDate?,
    val photoRevision: String?,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteSyncMembership(
    val remoteId: String,
    val petRemoteId: String,
    val userRemoteId: String,
    val role: String,
    val status: String,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteSyncEventType(
    val remoteId: String,
    val petRemoteId: String,
    val name: String,
    val category: String,
    val defaultDurationDays: Int?,
    val isActive: Boolean,
    val colorArgb: Long?,
    val iconKey: String?,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteSyncPetEvent(
    val remoteId: String,
    val petRemoteId: String,
    val eventTypeRemoteId: String,
    val eventDate: LocalDate,
    val dueDate: LocalDate?,
    val comment: String?,
    val notificationsEnabled: Boolean,
    val status: String,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteSyncWeightEntry(
    val remoteId: String,
    val petRemoteId: String,
    val date: LocalDate,
    val weightGrams: Int,
    val comment: String?,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class RemoteTombstone(
    val entityType: String,
    val remoteId: String,
    val deletedAt: LocalDateTime,
    val version: Long,
)

data class RemoteBootstrapSnapshot(
    val users: List<RemoteSyncUser>,
    val pets: List<RemoteSyncPet>,
    val memberships: List<RemoteSyncMembership>,
    val eventTypes: List<RemoteSyncEventType>,
    val petEvents: List<RemoteSyncPetEvent>,
    val weightEntries: List<RemoteSyncWeightEntry>,
)

data class RemoteBootstrapResponse(
    val cursor: String,
    val snapshot: RemoteBootstrapSnapshot,
)

data class RemoteChangesPayload(
    val users: List<RemoteSyncUser>,
    val pets: List<RemoteSyncPet>,
    val memberships: List<RemoteSyncMembership>,
    val eventTypes: List<RemoteSyncEventType>,
    val petEvents: List<RemoteSyncPetEvent>,
    val weightEntries: List<RemoteSyncWeightEntry>,
)

data class RemoteChangesResponse(
    val cursor: String,
    val hasMore: Boolean,
    val changes: RemoteChangesPayload,
    val tombstones: List<RemoteTombstone>,
)

data class RemotePushMutation(
    val clientMutationId: String,
    val entityId: String,
    val baseVersion: Long?,
    val entityType: String,
    val operation: String,
    val payloadJson: String?,
)

data class RemotePushRequest(
    val deviceId: String,
    val lastKnownCursor: String?,
    val mutations: List<RemotePushMutation>,
)

data class RemoteAcceptedMutation(
    val clientMutationId: String?,
    val entityType: String,
    val entityId: String,
    val version: Long,
)

data class RemoteConflict(
    val entityType: String,
    val entityId: String,
    val clientMutationId: String?,
    val baseVersion: Long?,
    val serverVersion: Long,
    val reason: String,
    val serverRecordJson: String?,
)

data class RemotePushResponse(
    val accepted: List<RemoteAcceptedMutation>,
    val conflicts: List<RemoteConflict>,
    val cursor: String,
    val requiresBootstrap: Boolean,
)
