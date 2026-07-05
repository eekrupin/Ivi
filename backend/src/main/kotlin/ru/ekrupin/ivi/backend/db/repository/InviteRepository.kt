package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.InviteRecord
import ru.ekrupin.ivi.backend.db.model.InviteStatusEntity
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.model.PetMembershipRecord
import ru.ekrupin.ivi.backend.db.schema.InvitesTable
import ru.ekrupin.ivi.backend.db.schema.PetMembershipsTable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class InviteRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(petId: UUID, createdByUserId: UUID, code: String, expiresAt: Instant): InviteRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            InvitesTable.insert {
                it[InvitesTable.id] = id
                it[InvitesTable.petId] = petId
                it[InvitesTable.createdByUserId] = createdByUserId
                it[InvitesTable.code] = code
                it[InvitesTable.status] = InviteStatusEntity.PENDING.name
                it[InvitesTable.expiresAt] = expiresAt.atOffset(ZoneOffset.UTC)
                it[InvitesTable.acceptedByUserId] = null
                it[InvitesTable.acceptedAt] = null
                it[InvitesTable.createdAt] = now
                it[InvitesTable.updatedAt] = now
            }
        }

        return findById(id) ?: error("Invite $id was not created")
    }

    fun findById(id: UUID): InviteRecord? = databaseFactory.dbQueryResult {
        InvitesTable.selectAll()
            .where { InvitesTable.id eq id }
            .singleOrNull()
            ?.toInviteRecord()
    }

    fun findByCode(code: String): InviteRecord? = databaseFactory.dbQueryResult {
        InvitesTable.selectAll()
            .where { InvitesTable.code eq code }
            .singleOrNull()
            ?.toInviteRecord()
    }

    fun accept(id: UUID, acceptedByUserId: UUID): InviteRecord? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updated = databaseFactory.dbQueryResult {
            InvitesTable.update({
                (InvitesTable.id eq id) and
                    (InvitesTable.status eq InviteStatusEntity.PENDING.name)
            }) {
                it[status] = InviteStatusEntity.ACCEPTED.name
                it[InvitesTable.acceptedByUserId] = acceptedByUserId
                it[acceptedAt] = now
                it[updatedAt] = now
            }
        }
        if (updated == 0) return null
        return findById(id)
    }

    fun acceptAndCreateMembership(id: UUID, acceptedByUserId: UUID): AcceptInviteMembershipResult {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val membershipId = UUID.randomUUID()

        return try {
            databaseFactory.dbQueryResult {
                val hasActiveMembership = PetMembershipsTable.selectAll()
                    .where {
                        (PetMembershipsTable.userId eq acceptedByUserId) and
                            (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
                    }
                    .limit(1)
                    .any()

                if (hasActiveMembership) {
                    return@dbQueryResult AcceptInviteMembershipResult.AlreadyBound
                }

                val updated = InvitesTable.update({
                    (InvitesTable.id eq id) and
                        (InvitesTable.status eq InviteStatusEntity.PENDING.name) and
                        (InvitesTable.expiresAt greaterEq now)
                }) {
                    it[status] = InviteStatusEntity.ACCEPTED.name
                    it[InvitesTable.acceptedByUserId] = acceptedByUserId
                    it[acceptedAt] = now
                    it[updatedAt] = now
                }

                if (updated == 0) {
                    return@dbQueryResult AcceptInviteMembershipResult.InviteNotActive
                }

                val invite = InvitesTable.selectAll()
                    .where { InvitesTable.id eq id }
                    .single()
                    .toInviteRecord()

                PetMembershipsTable.insert {
                    it[PetMembershipsTable.id] = membershipId
                    it[petId] = invite.petId
                    it[userId] = acceptedByUserId
                    it[role] = MembershipRoleEntity.MEMBER.name
                    it[PetMembershipsTable.status] = MembershipStatusEntity.ACTIVE.name
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                val membership = PetMembershipsTable.selectAll()
                    .where { PetMembershipsTable.id eq membershipId }
                    .single()
                    .toPetMembershipRecord()

                AcceptInviteMembershipResult.Accepted(invite = invite, membership = membership)
            }
        } catch (exception: ExposedSQLException) {
            if (exception.isMembershipUniqueViolation()) {
                AcceptInviteMembershipResult.AlreadyBound
            } else {
                throw exception
            }
        }
    }

    private fun ExposedSQLException.isMembershipUniqueViolation(): Boolean {
        return sqlState == POSTGRES_UNIQUE_VIOLATION &&
            generateSequence<Throwable>(this) { it.cause }
                .any { throwable ->
                    MEMBERSHIP_UNIQUE_INDEXES.any { indexName ->
                        throwable.message?.contains(indexName) == true
                    }
                }
    }

    private fun ResultRow.toInviteRecord(): InviteRecord = InviteRecord(
        id = this[InvitesTable.id].value,
        petId = this[InvitesTable.petId].value,
        createdByUserId = this[InvitesTable.createdByUserId].value,
        code = this[InvitesTable.code],
        status = InviteStatusEntity.valueOf(this[InvitesTable.status]),
        expiresAt = this[InvitesTable.expiresAt].toInstant(),
        acceptedByUserId = this[InvitesTable.acceptedByUserId]?.value,
        acceptedAt = this[InvitesTable.acceptedAt]?.toInstant(),
        createdAt = this[InvitesTable.createdAt].toInstant(),
        updatedAt = this[InvitesTable.updatedAt].toInstant(),
    )

    private fun ResultRow.toPetMembershipRecord(): PetMembershipRecord = PetMembershipRecord(
        id = this[PetMembershipsTable.id].value,
        petId = this[PetMembershipsTable.petId].value,
        userId = this[PetMembershipsTable.userId].value,
        role = MembershipRoleEntity.valueOf(this[PetMembershipsTable.role]),
        status = MembershipStatusEntity.valueOf(this[PetMembershipsTable.status]),
        createdAt = this[PetMembershipsTable.createdAt].toInstant(),
        updatedAt = this[PetMembershipsTable.updatedAt].toInstant(),
    )

    sealed interface AcceptInviteMembershipResult {
        data class Accepted(
            val invite: InviteRecord,
            val membership: PetMembershipRecord,
        ) : AcceptInviteMembershipResult

        data object AlreadyBound : AcceptInviteMembershipResult

        data object InviteNotActive : AcceptInviteMembershipResult
    }

    private companion object {
        const val POSTGRES_UNIQUE_VIOLATION = "23505"
        val MEMBERSHIP_UNIQUE_INDEXES = setOf(
            "uq_pet_memberships_pet_id_user_id",
            "uq_pet_memberships_user_id_active",
        )
    }
}
