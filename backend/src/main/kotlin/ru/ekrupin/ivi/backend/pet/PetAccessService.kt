package ru.ekrupin.ivi.backend.pet

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.auth.CreatePetRequest
import ru.ekrupin.ivi.backend.auth.CreatePetResponse
import ru.ekrupin.ivi.backend.auth.CurrentPetResponse
import ru.ekrupin.ivi.backend.auth.MeResponse
import ru.ekrupin.ivi.backend.auth.toLocalDateOrNull
import ru.ekrupin.ivi.backend.auth.toPetMembershipResponse
import ru.ekrupin.ivi.backend.auth.toPetResponse
import ru.ekrupin.ivi.backend.auth.toUserProfileResponse
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
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

        if (petMembershipRepository.hasAnyActiveMembership(userId)) {
            throw ApiException(
                status = HttpStatusCode.Conflict,
                code = "pet_already_exists_for_user",
                message = "В текущей V1-модели пользователь уже привязан к питомцу и не может создать второго",
            )
        }

        val pet = petRepository.create(
            name = request.name.trim(),
            birthDate = request.birthDate?.toLocalDateOrNull(),
        )
        petMembershipRepository.create(
            petId = pet.id,
            userId = userId,
            role = MembershipRoleEntity.OWNER,
        )
        return CreatePetResponse(pet = pet.toPetResponse())
    }

    fun getCurrentPet(userId: UUID): CurrentPetResponse {
        val membership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец не найден")

        val pet = petRepository.findById(membership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        return CurrentPetResponse(pet = pet.toPetResponse())
    }

    fun requireOwner(petId: UUID, userId: UUID) {
        val membership = petMembershipRepository.findActiveByPetAndUser(petId, userId)
            ?: throw ApiException(HttpStatusCode.Forbidden, "pet_forbidden", "Нет доступа к питомцу")

        if (membership.role != MembershipRoleEntity.OWNER || membership.status != MembershipStatusEntity.ACTIVE) {
            throw ApiException(HttpStatusCode.Forbidden, "owner_required", "Для этого действия нужен доступ OWNER")
        }
    }
}
