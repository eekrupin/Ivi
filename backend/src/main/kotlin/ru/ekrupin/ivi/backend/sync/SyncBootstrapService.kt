package ru.ekrupin.ivi.backend.sync

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import ru.ekrupin.ivi.backend.domain.PetDomainDataService
import java.time.Instant
import java.util.UUID

class SyncBootstrapService(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val petMembershipRepository: PetMembershipRepository,
    private val petDomainDataService: PetDomainDataService,
) {
    fun bootstrapForUser(userId: UUID): SyncBootstrapResponse {
        val currentMembership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец для bootstrap не найден")

        val pet = petRepository.findById(currentMembership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val memberships = petMembershipRepository.listActiveByPetId(pet.id)
        val users = userRepository.listByIds(memberships.map { it.userId }.toSet())
        val eventTypes = petDomainDataService.listEventTypes(pet.id, includeDeleted = true)
        val petEvents = petDomainDataService.listPetEvents(pet.id, includeDeleted = true)
        val weightEntries = petDomainDataService.listWeightEntries(pet.id, includeDeleted = true)

        return SyncBootstrapResponse(
            cursor = SyncCursorCodec.bootstrapCursor(),
            snapshot = SyncSnapshotResponse(
                users = users.map { it.toSyncUserProfileResponse() },
                pets = listOf(pet.toSyncPetResponse()),
                memberships = memberships.map { it.toSyncMembershipResponse() },
                eventTypes = eventTypes.map { it.toSyncEventTypeResponse() },
                petEvents = petEvents.map { it.toSyncPetEventResponse() },
                weightEntries = weightEntries.map { it.toSyncWeightEntryResponse() },
            ),
            notes = listOf(
                "bootstrap_includes_soft_deleted_records",
                "cursor_is_bootstrap_high_watermark",
            ),
        )
    }
}
