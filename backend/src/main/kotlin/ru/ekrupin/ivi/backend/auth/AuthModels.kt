package ru.ekrupin.ivi.backend.auth

import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.db.model.PetMembershipRecord
import ru.ekrupin.ivi.backend.db.model.PetRecord
import ru.ekrupin.ivi.backend.db.model.UserRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class AuthTokenPairResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String? = null,
    val email: String,
    val displayName: String,
    val createdAt: String,
)

@Serializable
data class AuthResponse(
    val tokens: AuthTokenPairResponse,
    val user: UserProfileResponse,
)

@Serializable
data class MeResponse(
    val user: UserProfileResponse,
    val currentPetId: String?,
    val memberships: List<PetMembershipResponse>,
)

@Serializable
data class CreatePetRequest(
    val name: String,
    val birthDate: String? = null,
)

@Serializable
data class PetResponse(
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
data class CreatePetResponse(
    val pet: PetResponse,
)

@Serializable
data class CurrentPetResponse(
    val pet: PetResponse,
)

@Serializable
data class PetMembershipResponse(
    val id: String,
    val version: Long = 1,
    val updatedAt: String,
    val deletedAt: String? = null,
    val petId: String,
    val userId: String,
    val role: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class CreateInviteRequest(
    val expiresInHours: Int? = null,
)

@Serializable
data class InviteResponse(
    val id: String,
    val petId: String,
    val code: String,
    val status: String,
    val expiresAt: String,
    val createdAt: String,
)

@Serializable
data class CreateInviteResponse(
    val invite: InviteResponse,
)

@Serializable
data class AcceptInviteRequest(
    val code: String,
)

@Serializable
data class AcceptInviteResponse(
    val invite: InviteResponse,
    val pet: PetResponse,
    val membership: PetMembershipResponse,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
)

data class AccessTokenClaims(
    val userId: UUID,
    val email: String,
)

fun UserRecord.toUserProfileResponse(): UserProfileResponse = UserProfileResponse(
    id = id.toString(),
    version = 1,
    updatedAt = updatedAt.toApiInstantString(),
    email = email,
    displayName = displayName,
    createdAt = createdAt.toApiInstantString(),
)

fun PetRecord.toPetResponse(): PetResponse = PetResponse(
    id = id.toString(),
    version = version,
    updatedAt = updatedAt.toApiInstantString(),
    deletedAt = deletedAt?.toApiInstantString(),
    name = name,
    birthDate = birthDate?.toString(),
    photoRevision = photoRevision,
    createdAt = createdAt.toApiInstantString(),
)

fun PetMembershipRecord.toPetMembershipResponse(): PetMembershipResponse = PetMembershipResponse(
    id = id.toString(),
    updatedAt = updatedAt.toApiInstantString(),
    petId = petId.toString(),
    userId = userId.toString(),
    role = role.name,
    status = status.name,
    createdAt = createdAt.toApiInstantString(),
)

fun ru.ekrupin.ivi.backend.db.model.InviteRecord.toInviteResponse(): InviteResponse = InviteResponse(
    id = id.toString(),
    petId = petId.toString(),
    code = code,
    status = status.name,
    expiresAt = expiresAt.toApiInstantString(),
    createdAt = createdAt.toApiInstantString(),
)

internal fun String.toLocalDateOrNull(): LocalDate? = if (isBlank()) null else LocalDate.parse(this)

private fun Instant.toApiInstantString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(atOffset(ZoneOffset.UTC))
