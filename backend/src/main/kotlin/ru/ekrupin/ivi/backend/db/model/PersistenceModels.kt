package ru.ekrupin.ivi.backend.db.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class MembershipRoleEntity {
    OWNER,
    MEMBER,
}

enum class MembershipStatusEntity {
    ACTIVE,
    REVOKED,
}

enum class InviteStatusEntity {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED,
}

data class UserRecord(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PetRecord(
    val id: UUID,
    val name: String,
    val birthDate: LocalDate?,
    val photoRevision: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val version: Long,
)

data class PetMembershipRecord(
    val id: UUID,
    val petId: UUID,
    val userId: UUID,
    val role: MembershipRoleEntity,
    val status: MembershipStatusEntity,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class InviteRecord(
    val id: UUID,
    val petId: UUID,
    val createdByUserId: UUID,
    val code: String,
    val status: InviteStatusEntity,
    val expiresAt: Instant,
    val acceptedByUserId: UUID?,
    val acceptedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
