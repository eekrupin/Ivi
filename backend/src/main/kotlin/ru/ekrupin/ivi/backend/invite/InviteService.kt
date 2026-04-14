package ru.ekrupin.ivi.backend.invite

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.auth.AcceptInviteRequest
import ru.ekrupin.ivi.backend.auth.AcceptInviteResponse
import ru.ekrupin.ivi.backend.auth.CreateInviteRequest
import ru.ekrupin.ivi.backend.auth.CreateInviteResponse
import ru.ekrupin.ivi.backend.auth.toInviteResponse
import ru.ekrupin.ivi.backend.auth.toPetMembershipResponse
import ru.ekrupin.ivi.backend.auth.toPetResponse
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.InviteStatusEntity
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.repository.InviteRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

class InviteService(
    private val petRepository: PetRepository,
    private val petMembershipRepository: PetMembershipRepository,
    private val inviteRepository: InviteRepository,
) {
    private val secureRandom = SecureRandom()

    fun createInvite(petId: UUID, currentUserId: UUID, request: CreateInviteRequest): CreateInviteResponse {
        val pet = petRepository.findById(petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val membership = petMembershipRepository.findActiveByPetAndUser(petId, currentUserId)
            ?: throw ApiException(HttpStatusCode.Forbidden, "pet_forbidden", "Нет доступа к питомцу")

        if (membership.role != MembershipRoleEntity.OWNER) {
            throw ApiException(HttpStatusCode.Forbidden, "owner_required", "Приглашение может создать только OWNER")
        }

        val expiresInHours = request.expiresInHours ?: 72
        if (expiresInHours !in 1..168) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_invite_ttl", "Срок действия приглашения должен быть от 1 до 168 часов")
        }

        val invite = inviteRepository.create(
            petId = pet.id,
            createdByUserId = currentUserId,
            code = generateInviteCode(),
            expiresAt = Instant.now().plusSeconds(expiresInHours.toLong() * 3600),
        )
        return CreateInviteResponse(invite = invite.toInviteResponse())
    }

    fun acceptInvite(currentUserId: UUID, request: AcceptInviteRequest): AcceptInviteResponse {
        if (request.code.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_invite_code", "Код приглашения обязателен")
        }

        val invite = inviteRepository.findByCode(request.code)
            ?: throw ApiException(HttpStatusCode.NotFound, "invite_not_found", "Приглашение не найдено")

        if (invite.status != InviteStatusEntity.PENDING) {
            throw ApiException(HttpStatusCode.Conflict, "invite_not_active", "Приглашение уже использовано или недействительно")
        }

        if (invite.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatusCode.Conflict, "invite_expired", "Срок действия приглашения истек")
        }

        val existingMembership = petMembershipRepository.findCurrentActiveMembership(currentUserId)
        if (existingMembership != null) {
            throw ApiException(
                HttpStatusCode.Conflict,
                "user_already_bound_to_pet",
                "В текущей V1-модели пользователь уже привязан к питомцу",
            )
        }

        val pet = petRepository.findById(invite.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val membership = petMembershipRepository.create(
            petId = invite.petId,
            userId = currentUserId,
            role = MembershipRoleEntity.MEMBER,
        )
        val acceptedInvite = inviteRepository.accept(invite.id, currentUserId)
            ?: throw ApiException(HttpStatusCode.Conflict, "invite_accept_failed", "Не удалось принять приглашение")

        return AcceptInviteResponse(
            invite = acceptedInvite.toInviteResponse(),
            pet = pet.toPetResponse(),
            membership = membership.toPetMembershipResponse(),
        )
    }

    private fun generateInviteCode(): String {
        val bytes = ByteArray(18)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
