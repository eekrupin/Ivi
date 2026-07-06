package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.InviteStatusEntity
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.model.PetMembershipRecord
import ru.ekrupin.ivi.backend.db.model.PetRecord
import ru.ekrupin.ivi.backend.db.schema.InvitesTable
import ru.ekrupin.ivi.backend.db.schema.PetMembershipsTable
import ru.ekrupin.ivi.backend.db.schema.PetsTable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PetMembershipRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(petId: UUID, userId: UUID, role: MembershipRoleEntity): PetMembershipRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            PetMembershipsTable.insert {
                it[PetMembershipsTable.id] = id
                it[PetMembershipsTable.petId] = petId
                it[PetMembershipsTable.userId] = userId
                it[PetMembershipsTable.role] = role.name
                it[PetMembershipsTable.status] = MembershipStatusEntity.ACTIVE.name
                it[PetMembershipsTable.createdAt] = now
                it[PetMembershipsTable.updatedAt] = now
            }
        }

        return findById(id) ?: error("Membership $id was not created")
    }

    fun findById(id: UUID): PetMembershipRecord? = databaseFactory.dbQueryResult {
        PetMembershipsTable.selectAll()
            .where { PetMembershipsTable.id eq id }
            .singleOrNull()
            ?.toPetMembershipRecord()
    }

    fun listActiveByUserId(userId: UUID): List<PetMembershipRecord> = databaseFactory.dbQueryResult {
        PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.userId eq userId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }
            .map { it.toPetMembershipRecord() }
    }

    fun hasAnyActiveMembership(userId: UUID): Boolean = listActiveByUserId(userId).isNotEmpty()

    fun findCurrentActiveMembership(userId: UUID): PetMembershipRecord? = listActiveByUserId(userId).firstOrNull()

    fun findActiveByPetAndUser(petId: UUID, userId: UUID): PetMembershipRecord? = databaseFactory.dbQueryResult {
        PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.petId eq petId) and
                    (PetMembershipsTable.userId eq userId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }
            .singleOrNull()
            ?.toPetMembershipRecord()
    }

    fun revokeActiveByIdAndUserId(id: UUID, userId: UUID): PetMembershipRecord? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updatedRows = databaseFactory.dbQueryResult {
            PetMembershipsTable.update({
                (PetMembershipsTable.id eq id) and
                    (PetMembershipsTable.userId eq userId) and
                    (PetMembershipsTable.role eq MembershipRoleEntity.MEMBER.name) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }) {
                it[PetMembershipsTable.status] = MembershipStatusEntity.REVOKED.name
                it[PetMembershipsTable.updatedAt] = now
            }
        }

        return if (updatedRows == 1) findById(id) else null
    }

    fun listActiveByPetId(petId: UUID): List<PetMembershipRecord> = databaseFactory.dbQueryResult {
        PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.petId eq petId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }
            .map { it.toPetMembershipRecord() }
    }

    fun listByPetIdChangedBetween(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<PetMembershipRecord> {
        val since = sinceExclusive.atOffset(ZoneOffset.UTC)
        val until = untilInclusive.atOffset(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            PetMembershipsTable.selectAll()
                .where {
                    (PetMembershipsTable.petId eq petId) and
                        (PetMembershipsTable.updatedAt greater since) and
                        (PetMembershipsTable.updatedAt lessEq until)
                }
                .map { it.toPetMembershipRecord() }
        }
    }

    fun leaveOwnerCurrentPet(
        ownerUserId: UUID,
        transferOwnerToUserId: UUID?,
        deletePet: Boolean,
    ): OwnerLeaveResult = try {
        databaseFactory.dbQueryResult {
        val ownerMembership = PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.userId eq ownerUserId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }
            .singleOrNull()
            ?.toPetMembershipRecord()
            ?: return@dbQueryResult OwnerLeaveResult.NoCurrentPet

        val pet = PetsTable.selectAll()
            .where {
                (PetsTable.id eq ownerMembership.petId) and
                    PetsTable.deletedAt.isNull()
            }
            .forUpdate()
            .singleOrNull()
            ?.toPetRecord()
            ?: return@dbQueryResult OwnerLeaveResult.NoCurrentPet

        val currentOwnerMembership = PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.id eq ownerMembership.id) and
                    (PetMembershipsTable.userId eq ownerUserId) and
                    (PetMembershipsTable.petId eq ownerMembership.petId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name) and
                    (PetMembershipsTable.role eq MembershipRoleEntity.OWNER.name)
            }
            .singleOrNull()
            ?.toPetMembershipRecord()

        if (currentOwnerMembership == null) {
            return@dbQueryResult OwnerLeaveResult.NotOwner
        }

        val activeMemberships = PetMembershipsTable.selectAll()
            .where {
                (PetMembershipsTable.petId eq currentOwnerMembership.petId) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }
            .map { it.toPetMembershipRecord() }
        val otherActiveMemberships = activeMemberships.filter { it.userId != ownerUserId }
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        if (transferOwnerToUserId != null) {
            val candidate = otherActiveMemberships.singleOrNull {
                it.userId == transferOwnerToUserId && it.role == MembershipRoleEntity.MEMBER
            } ?: return@dbQueryResult OwnerLeaveResult.InvalidTransferCandidate

            val promoted = PetMembershipsTable.update({
                (PetMembershipsTable.id eq candidate.id) and
                    (PetMembershipsTable.userId eq transferOwnerToUserId) and
                    (PetMembershipsTable.petId eq currentOwnerMembership.petId) and
                    (PetMembershipsTable.role eq MembershipRoleEntity.MEMBER.name) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }) {
                it[PetMembershipsTable.role] = MembershipRoleEntity.OWNER.name
                it[PetMembershipsTable.updatedAt] = now
            }

            if (promoted != 1) {
                return@dbQueryResult OwnerLeaveResult.InvalidTransferCandidate
            }

            val revokedOwner = PetMembershipsTable.update({
                (PetMembershipsTable.id eq currentOwnerMembership.id) and
                    (PetMembershipsTable.userId eq ownerUserId) and
                    (PetMembershipsTable.petId eq currentOwnerMembership.petId) and
                    (PetMembershipsTable.role eq MembershipRoleEntity.OWNER.name) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }) {
                it[PetMembershipsTable.status] = MembershipStatusEntity.REVOKED.name
                it[PetMembershipsTable.updatedAt] = now
            }

            if (revokedOwner != 1) {
                throw OwnerTransferRaceException()
            }

            return@dbQueryResult OwnerLeaveResult.Transferred(
                revokedOwnerMembership = PetMembershipsTable.selectAll()
                    .where { PetMembershipsTable.id eq currentOwnerMembership.id }
                    .single()
                    .toPetMembershipRecord(),
                newOwnerMembership = PetMembershipsTable.selectAll()
                    .where { PetMembershipsTable.id eq candidate.id }
                    .single()
                    .toPetMembershipRecord(),
                pet = pet,
            )
        }

        if (deletePet) {
            if (otherActiveMemberships.isNotEmpty()) {
                return@dbQueryResult OwnerLeaveResult.DeleteRequiresNoMembers
            }

            PetsTable.update({
                (PetsTable.id eq currentOwnerMembership.petId) and
                    PetsTable.deletedAt.isNull()
            }) {
                it[PetsTable.deletedAt] = now
                it[PetsTable.updatedAt] = now
                it[PetsTable.version] = pet.version + 1L
            }
            InvitesTable.update({
                (InvitesTable.petId eq currentOwnerMembership.petId) and
                    (InvitesTable.status eq InviteStatusEntity.PENDING.name)
            }) {
                it[InvitesTable.status] = InviteStatusEntity.REVOKED.name
                it[InvitesTable.updatedAt] = now
            }
            PetMembershipsTable.update({
                (PetMembershipsTable.id eq currentOwnerMembership.id) and
                    (PetMembershipsTable.userId eq ownerUserId) and
                    (PetMembershipsTable.petId eq currentOwnerMembership.petId) and
                    (PetMembershipsTable.role eq MembershipRoleEntity.OWNER.name) and
                    (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
            }) {
                it[PetMembershipsTable.status] = MembershipStatusEntity.REVOKED.name
                it[PetMembershipsTable.updatedAt] = now
            }

            return@dbQueryResult OwnerLeaveResult.DeletedPet(
                revokedOwnerMembership = PetMembershipsTable.selectAll()
                    .where { PetMembershipsTable.id eq currentOwnerMembership.id }
                    .single()
                    .toPetMembershipRecord(),
                pet = PetsTable.selectAll()
                    .where { PetsTable.id eq currentOwnerMembership.petId }
                    .single()
                    .toPetRecord(),
            )
        }

        OwnerLeaveResult.RequiresAction
        }
    } catch (_: OwnerTransferRaceException) {
        OwnerLeaveResult.InvalidTransferCandidate
    }

    private fun ResultRow.toPetMembershipRecord(): PetMembershipRecord = PetMembershipRecord(
        id = this[PetMembershipsTable.id].value,
        petId = this[PetMembershipsTable.petId].value,
        userId = this[PetMembershipsTable.userId].value,
        role = MembershipRoleEntity.valueOf(this[PetMembershipsTable.role]),
        status = MembershipStatusEntity.valueOf(this[PetMembershipsTable.status]),
        createdAt = this[PetMembershipsTable.createdAt].toInstant(),
        updatedAt = this[PetMembershipsTable.updatedAt].toInstant(),
    )

    private fun ResultRow.toPetRecord(): PetRecord = PetRecord(
        id = this[PetsTable.id].value,
        name = this[PetsTable.name],
        birthDate = this[PetsTable.birthDate],
        photoRevision = this[PetsTable.photoRevision],
        createdAt = this[PetsTable.createdAt].toInstant(),
        updatedAt = this[PetsTable.updatedAt].toInstant(),
        deletedAt = this[PetsTable.deletedAt]?.toInstant(),
        version = this[PetsTable.version],
    )

    sealed interface OwnerLeaveResult {
        data object NoCurrentPet : OwnerLeaveResult

        data object NotOwner : OwnerLeaveResult

        data object RequiresAction : OwnerLeaveResult

        data object DeleteRequiresNoMembers : OwnerLeaveResult

        data object InvalidTransferCandidate : OwnerLeaveResult

        data class Transferred(
            val revokedOwnerMembership: PetMembershipRecord,
            val newOwnerMembership: PetMembershipRecord,
            val pet: PetRecord,
        ) : OwnerLeaveResult

        data class DeletedPet(
            val revokedOwnerMembership: PetMembershipRecord,
            val pet: PetRecord,
        ) : OwnerLeaveResult
    }

    private class OwnerTransferRaceException : RuntimeException()
}
