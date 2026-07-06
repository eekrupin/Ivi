package ru.ekrupin.ivi.backend.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import ru.ekrupin.ivi.backend.BackendPostgresTest
import ru.ekrupin.ivi.backend.db.model.InviteStatusEntity
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.repository.InviteRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository.OwnerLeaveResult
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import java.time.Instant

class PetOwnershipRepositoryTest : BackendPostgresTest() {
    private val users by lazy { UserRepository(databaseFactory) }
    private val pets by lazy { PetRepository(databaseFactory) }
    private val memberships by lazy { PetMembershipRepository(databaseFactory) }
    private val invites by lazy { InviteRepository(databaseFactory) }

    @Test
    fun `created pet always has active owner`() {
        val owner = user("owner")

        val result = pets.createWithOwnerMembership("Иви", null, owner.id)

        val pet = assertIs<PetRepository.CreatePetWithOwnerResult.Created>(result).pet
        val activeMemberships = memberships.listActiveByPetId(pet.id)
        assertEquals(1, activeMemberships.size)
        assertEquals(owner.id, activeMemberships.single().userId)
        assertEquals(MembershipRoleEntity.OWNER, activeMemberships.single().role)
    }

    @Test
    fun `member leaves without deleting pet for owner`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.email, owner.id)
        memberships.create(pet.id, member.id, MembershipRoleEntity.MEMBER)

        val revoked = memberships.revokeActiveByIdAndUserId(
            id = memberships.findActiveByPetAndUser(pet.id, member.id)!!.id,
            userId = member.id,
        )

        assertNotNull(revoked)
        assertEquals(MembershipStatusEntity.REVOKED, revoked.status)
        assertNull(pets.findById(pet.id)!!.deletedAt)
        assertEquals(MembershipRoleEntity.OWNER, memberships.findActiveByPetAndUser(pet.id, owner.id)!!.role)
    }

    @Test
    fun `owner cannot leave pet without transfer or delete action`() {
        val owner = user("owner")
        createPet(owner.email, owner.id)

        val result = memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = null, deletePet = false)

        assertEquals(OwnerLeaveResult.RequiresAction, result)
        assertEquals(MembershipRoleEntity.OWNER, memberships.findCurrentActiveMembership(owner.id)!!.role)
    }

    @Test
    fun `owner transfer promotes active member and revokes old owner`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.email, owner.id)
        memberships.create(pet.id, member.id, MembershipRoleEntity.MEMBER)

        val result = memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = member.id, deletePet = false)

        assertIs<OwnerLeaveResult.Transferred>(result)
        assertNull(memberships.findCurrentActiveMembership(owner.id))
        assertEquals(MembershipRoleEntity.OWNER, memberships.findCurrentActiveMembership(member.id)!!.role)
        assertNull(pets.findById(pet.id)!!.deletedAt)
    }

    @Test
    fun `owner transfer rejects missing or inactive member`() {
        val owner = user("owner")
        val member = user("member")
        val outsider = user("outsider")
        val pet = createPet(owner.email, owner.id)
        val memberMembership = memberships.create(pet.id, member.id, MembershipRoleEntity.MEMBER)
        memberships.revokeActiveByIdAndUserId(memberMembership.id, member.id)

        assertEquals(
            OwnerLeaveResult.InvalidTransferCandidate,
            memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = member.id, deletePet = false),
        )
        assertEquals(
            OwnerLeaveResult.InvalidTransferCandidate,
            memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = outsider.id, deletePet = false),
        )
        assertEquals(MembershipRoleEntity.OWNER, memberships.findCurrentActiveMembership(owner.id)!!.role)
    }

    @Test
    fun `single owner can delete pet and pending invites are revoked`() {
        val owner = user("owner")
        val pet = createPet(owner.email, owner.id)
        val invite = invites.create(pet.id, owner.id, "INVITE", Instant.now().plusSeconds(3600))

        val result = memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = null, deletePet = true)

        assertIs<OwnerLeaveResult.DeletedPet>(result)
        assertNotNull(pets.findById(pet.id)!!.deletedAt)
        assertNull(memberships.findCurrentActiveMembership(owner.id))
        assertEquals(InviteStatusEntity.REVOKED, invites.findById(invite.id)!!.status)
    }

    @Test
    fun `owner cannot delete pet while other members are active`() {
        val owner = user("owner")
        val member = user("member")
        val pet = createPet(owner.email, owner.id)
        memberships.create(pet.id, member.id, MembershipRoleEntity.MEMBER)

        val result = memberships.leaveOwnerCurrentPet(owner.id, transferOwnerToUserId = null, deletePet = true)

        assertEquals(OwnerLeaveResult.DeleteRequiresNoMembers, result)
        assertNull(pets.findById(pet.id)!!.deletedAt)
        assertEquals(MembershipRoleEntity.OWNER, memberships.findCurrentActiveMembership(owner.id)!!.role)
        assertEquals(MembershipRoleEntity.MEMBER, memberships.findCurrentActiveMembership(member.id)!!.role)
    }

    private fun user(name: String) = users.create("$name@example.test", "hash", name)

    private fun createPet(email: String, ownerId: java.util.UUID) =
        assertIs<PetRepository.CreatePetWithOwnerResult.Created>(
            pets.createWithOwnerMembership("Питомец $email", null, ownerId),
        ).pet
}
