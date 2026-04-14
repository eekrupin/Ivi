package ru.ekrupin.ivi.backend.sync

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.EventTypeRecord
import ru.ekrupin.ivi.backend.db.model.PetEventRecord
import ru.ekrupin.ivi.backend.db.model.WeightEntryRecord
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import ru.ekrupin.ivi.backend.domain.PetDomainDataService
import java.time.Instant
import java.util.UUID

@Serializable
data class SyncChangesResponse(
    val cursor: String,
    val hasMore: Boolean,
    val changes: SyncEntityChangesResponse,
    val tombstones: List<TombstoneResponse>,
)

@Serializable
data class SyncEntityChangesResponse(
    val users: List<SyncUserProfileResponse>,
    val pets: List<SyncPetResponse>,
    val memberships: List<SyncMembershipResponse>,
    val eventTypes: List<SyncEventTypeResponse>,
    val petEvents: List<SyncPetEventResponse>,
    val weightEntries: List<SyncWeightEntryResponse>,
)

@Serializable
data class TombstoneResponse(
    val entityType: String,
    val id: String,
    val deletedAt: String,
    val version: Long,
)

class SyncChangesService(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val petMembershipRepository: PetMembershipRepository,
    private val petDomainDataService: PetDomainDataService,
) {
    fun changesForUser(userId: UUID, rawCursor: String): SyncChangesResponse {
        val currentMembership = petMembershipRepository.findCurrentActiveMembership(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "current_pet_not_found", "Текущий питомец для sync changes не найден")

        val pet = petRepository.findById(currentMembership.petId)
            ?: throw ApiException(HttpStatusCode.NotFound, "pet_not_found", "Питомец не найден")

        val cursor = SyncCursorCodec.decode(rawCursor)
        val since = Instant.ofEpochMilli(cursor.epochMillis)
        val until = Instant.now()

        val activeMemberships = petMembershipRepository.listActiveByPetId(pet.id)
        val changedMemberships = petMembershipRepository.listActiveByPetIdChangedBetween(pet.id, since, until)
        val changedPetRecords = petRepository.listChangedByIds(listOf(pet.id), since, until)

        val membershipUserIds = activeMemberships.map { it.userId }.toSet()
        val changedMembershipUserIds = changedMemberships.map { it.userId }.toSet()
        val directlyChangedUsers = userRepository.listChangedByIds(membershipUserIds, since, until)
        val membershipReferencedUsers = userRepository.listByIds(changedMembershipUserIds)
        val changedUsers = (directlyChangedUsers + membershipReferencedUsers).distinctBy { it.id }

        val changedEventTypes = petDomainDataService.listChangedEventTypes(pet.id, since, until)
        val changedPetEvents = petDomainDataService.listChangedPetEvents(pet.id, since, until)
        val changedWeightEntries = petDomainDataService.listChangedWeightEntries(pet.id, since, until)

        return SyncChangesResponse(
            cursor = SyncCursorCodec.changesCursor(until),
            hasMore = false,
            changes = SyncEntityChangesResponse(
                users = changedUsers.map { it.toSyncUserProfileResponse() },
                pets = changedPetRecords.filter { it.deletedAt == null }.map { it.toSyncPetResponse() },
                memberships = changedMemberships.map { it.toSyncMembershipResponse() },
                eventTypes = changedEventTypes.filter { it.deletedAt == null }.map { it.toSyncEventTypeResponse() },
                petEvents = changedPetEvents.filter { it.deletedAt == null }.map { it.toSyncPetEventResponse() },
                weightEntries = changedWeightEntries.filter { it.deletedAt == null }.map { it.toSyncWeightEntryResponse() },
            ),
            tombstones = buildList {
                addAll(changedPetRecords.toPetTombstones())
                addAll(changedEventTypes.toEventTypeTombstones())
                addAll(changedPetEvents.toPetEventTombstones())
                addAll(changedWeightEntries.toWeightEntryTombstones())
            },
        )
    }

    private fun List<EventTypeRecord>.toEventTypeTombstones(): List<TombstoneResponse> =
        filter { it.deletedAt != null }.map {
            TombstoneResponse(
                entityType = "EVENT_TYPE",
                id = it.id.toString(),
                deletedAt = it.deletedAt!!.toApiInstantString(),
                version = it.version,
            )
        }

    private fun List<PetEventRecord>.toPetEventTombstones(): List<TombstoneResponse> =
        filter { it.deletedAt != null }.map {
            TombstoneResponse(
                entityType = "PET_EVENT",
                id = it.id.toString(),
                deletedAt = it.deletedAt!!.toApiInstantString(),
                version = it.version,
            )
        }

    private fun List<WeightEntryRecord>.toWeightEntryTombstones(): List<TombstoneResponse> =
        filter { it.deletedAt != null }.map {
            TombstoneResponse(
                entityType = "WEIGHT_ENTRY",
                id = it.id.toString(),
                deletedAt = it.deletedAt!!.toApiInstantString(),
                version = it.version,
            )
        }

    private fun List<ru.ekrupin.ivi.backend.db.model.PetRecord>.toPetTombstones(): List<TombstoneResponse> =
        filter { it.deletedAt != null }.map {
            TombstoneResponse(
                entityType = "PET",
                id = it.id.toString(),
                deletedAt = it.deletedAt!!.toApiInstantString(),
                version = it.version,
            )
        }
}
