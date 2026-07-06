package ru.ekrupin.ivi.backend.pet

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.auth.CreatePetRequest
import ru.ekrupin.ivi.backend.auth.CreatePetResponse
import ru.ekrupin.ivi.backend.auth.CurrentPetResponse
import ru.ekrupin.ivi.backend.auth.LeavePetOptionsResponse
import ru.ekrupin.ivi.backend.auth.LeavePetRequest
import ru.ekrupin.ivi.backend.auth.LeavePetResponse
import ru.ekrupin.ivi.backend.auth.MeResponse
import ru.ekrupin.ivi.backend.auth.toLocalDateOrNull
import ru.ekrupin.ivi.backend.auth.toPetMembershipResponse
import ru.ekrupin.ivi.backend.auth.toPetResponse
import ru.ekrupin.ivi.backend.auth.toUserProfileResponse
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository.OwnerLeaveResult
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository.CreatePetWithOwnerResult
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import java.util.UUID

class PetAccessService(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val petMembershipRepository: PetMembershipRepository,
) {
    fun getMe(userId: UUID): MeResponse {
        val user = userRepository.findById(userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "user_not_found", "Пользователь не найден")

        val memberships = petMembershipRepository.listActiveByUserId(userId)
        return MeResponse(
            user = user.toUserProfileResponse(),
            currentPetId = memberships.firstOrNull()?.petId?.toString(),
            memberships = memberships.map { it.toPetMembershipResponse() },
        )
    }

    fun createPet(userId: UUID, request: CreatePetRequest): CreatePetResponse {
        if (request.name.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_pet_name", "Имя питомца обязательно")
        }

        val result = petRepository.createWithOwnerMembership(
            name = request.name.trim(),
            birthDate = request.birthDate?.toLocalDateOrNull(),
            ownerUserId = userId,
        )

        val pet = when (result) {
            is CreatePetWithOwnerResult.Created -> result.pet
            CreatePetWithOwnerResult.AlreadyBound -> throw ApiException(
                status = HttpStatusCode.Conflict,
                code = "pet_already_exists_for_user",
                message = "В текущей V1-модели пользователь уже привязан к питомцу и не может создать второго",
            )
        }
        return CreatePetResponse(pet = pet.toPetResponse())
    }

    fun getCurrentPet(userId: UUID): CurrentPetResponse {
        val membership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец не найден")

        val pet = petRepository.findById(membership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        return CurrentPetResponse(
            pet = pet.toPetResponse(),
            membership = membership.toPetMembershipResponse(),
        )
    }

    fun getCurrentPetLeaveOptions(userId: UUID): LeavePetOptionsResponse {
        val membership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец не найден")

        val pet = petRepository.findById(membership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val activeMemberships = petMembershipRepository.listActiveByPetId(membership.petId)
        val transferCandidates = if (membership.role == MembershipRoleEntity.OWNER) {
            val candidateUserIds = activeMemberships
                .filter { it.userId != userId && it.role == MembershipRoleEntity.MEMBER }
                .map { it.userId }
            userRepository.listByIds(candidateUserIds)
        } else {
            emptyList()
        }

        return LeavePetOptionsResponse(
            pet = pet.toPetResponse(),
            membership = membership.toPetMembershipResponse(),
            transferCandidates = transferCandidates.map { it.toUserProfileResponse() },
            canDeletePet = membership.role == MembershipRoleEntity.OWNER &&
                activeMemberships.none { it.userId != userId },
        )
    }

    fun leaveCurrentPet(userId: UUID, request: LeavePetRequest?): LeavePetResponse {
        val membership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец не найден")

        if (membership.role != MembershipRoleEntity.OWNER) {
            val revokedMembership = petMembershipRepository.revokeActiveByIdAndUserId(membership.id, userId)
                ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец не найден")

            return LeavePetResponse(
                membership = revokedMembership.toPetMembershipResponse(),
                action = "LEFT",
            )
        }

        val transferOwnerToUserId = request?.transferOwnerToUserId?.let { rawUserId ->
            runCatching { UUID.fromString(rawUserId) }.getOrElse {
                throw ApiException(
                    HttpStatusCode.Conflict,
                    "invalid_owner_transfer_candidate",
                    "Нельзя передать владение выбранному пользователю",
                )
            }
        }

        return when (val result = petMembershipRepository.leaveOwnerCurrentPet(userId, transferOwnerToUserId, request?.deletePet == true)) {
            OwnerLeaveResult.NoCurrentPet -> throw ApiException(
                HttpStatusCode.NotFound,
                "current_pet_not_found",
                "Текущий питомец не найден",
            )
            OwnerLeaveResult.NotOwner -> throw ApiException(
                HttpStatusCode.Conflict,
                "owner_leave_requires_action",
                "Для выхода владельца нужно выбрать действие",
            )
            OwnerLeaveResult.RequiresAction -> throw ApiException(
                HttpStatusCode.Conflict,
                "owner_leave_requires_action",
                "Передайте владение другому участнику или удалите питомца без участников",
            )
            OwnerLeaveResult.DeleteRequiresNoMembers -> throw ApiException(
                HttpStatusCode.Conflict,
                "owner_delete_requires_no_members",
                "Нельзя удалить питомца, пока есть другие участники",
            )
            OwnerLeaveResult.InvalidTransferCandidate -> throw ApiException(
                HttpStatusCode.Conflict,
                "invalid_owner_transfer_candidate",
                "Нельзя передать владение выбранному пользователю",
            )
            is OwnerLeaveResult.Transferred -> LeavePetResponse(
                membership = result.revokedOwnerMembership.toPetMembershipResponse(),
                pet = result.pet.toPetResponse(),
                newOwnerMembership = result.newOwnerMembership.toPetMembershipResponse(),
                action = "TRANSFERRED_OWNERSHIP",
            )
            is OwnerLeaveResult.DeletedPet -> LeavePetResponse(
                membership = result.revokedOwnerMembership.toPetMembershipResponse(),
                pet = result.pet.toPetResponse(),
                action = "DELETED_PET",
            )
        }
    }

    fun requireOwner(petId: UUID, userId: UUID) {
        val membership = petMembershipRepository.findActiveByPetAndUser(petId, userId)
            ?: throw ApiException(HttpStatusCode.Forbidden, "pet_forbidden", "Нет доступа к питомцу")

        if (membership.role != MembershipRoleEntity.OWNER || membership.status != MembershipStatusEntity.ACTIVE) {
            throw ApiException(HttpStatusCode.Forbidden, "owner_required", "Для этого действия нужен доступ OWNER")
        }
    }
}
