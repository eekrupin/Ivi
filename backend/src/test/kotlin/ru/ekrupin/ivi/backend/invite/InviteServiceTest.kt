package ru.ekrupin.ivi.backend.invite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import ru.ekrupin.ivi.backend.BackendPostgresTest
import ru.ekrupin.ivi.backend.auth.AcceptInviteRequest
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.repository.InviteRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import java.time.Instant

class InviteServiceTest : BackendPostgresTest() {
    private val users by lazy { UserRepository(databaseFactory) }
    private val pets by lazy { PetRepository(databaseFactory) }
    private val memberships by lazy { PetMembershipRepository(databaseFactory) }
    private val invites by lazy { InviteRepository(databaseFactory) }
    private val service by lazy { InviteService(pets, memberships, invites) }

    @Test
    fun `second user accepts invite and repeated accept by same user is idempotent`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.id)
        invites.create(pet.id, owner.id, "CODE", Instant.now().plusSeconds(3600))

        val first = service.acceptInvite(member.id, AcceptInviteRequest(code = "CODE"))
        val second = service.acceptInvite(member.id, AcceptInviteRequest(code = "CODE"))

        assertEquals(pet.id.toString(), first.pet.id)
        assertEquals(first.membership.id, second.membership.id)
        assertEquals("MEMBER", second.membership.role)
    }

    @Test
    fun `already bound user cannot accept invite to another pet`() {
        val owner = user("owner")
        val boundUser = user("bound")
        val firstPet = createPet(boundUser.id)
        val secondPet = createPet(owner.id)
        invites.create(secondPet.id, owner.id, "SECOND", Instant.now().plusSeconds(3600))

        val error = assertFailsWith<ApiException> {
            service.acceptInvite(boundUser.id, AcceptInviteRequest(code = "SECOND"))
        }

        assertEquals("user_already_bound_to_pet", error.code)
        assertEquals(firstPet.id, memberships.findCurrentActiveMembership(boundUser.id)!!.petId)
    }

    @Test
    fun `expired invite is rejected clearly`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.id)
        invites.create(pet.id, owner.id, "OLD", Instant.now().minusSeconds(1))

        val error = assertFailsWith<ApiException> {
            service.acceptInvite(member.id, AcceptInviteRequest(code = "OLD"))
        }

        assertEquals("invite_expired", error.code)
    }

    @Test
    fun `invite to deleted pet cannot be accepted`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.id)
        invites.create(pet.id, owner.id, "STALE", Instant.now().plusSeconds(3600))
        assertIs<PetMembershipRepository.OwnerLeaveResult.DeletedPet>(
            memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = null, deletePet = true),
        )

        val error = assertFailsWith<ApiException> {
            service.acceptInvite(member.id, AcceptInviteRequest(code = "STALE"))
        }

        assertEquals("invite_not_active", error.code)
    }

    private fun user(name: String) = users.create("$name@example.test", "hash", name)

    private fun createPet(ownerId: java.util.UUID) =
        assertIs<PetRepository.CreatePetWithOwnerResult.Created>(
            pets.createWithOwnerMembership("Иви", null, ownerId),
        ).pet
}
