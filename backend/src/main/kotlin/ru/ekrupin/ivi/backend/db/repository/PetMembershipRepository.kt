package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.model.PetMembershipRecord
import ru.ekrupin.ivi.backend.db.schema.PetMembershipsTable
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

    private fun ResultRow.toPetMembershipRecord(): PetMembershipRecord = PetMembershipRecord(
        id = this[PetMembershipsTable.id].value,
        petId = this[PetMembershipsTable.petId].value,
        userId = this[PetMembershipsTable.userId].value,
        role = MembershipRoleEntity.valueOf(this[PetMembershipsTable.role]),
        status = MembershipStatusEntity.valueOf(this[PetMembershipsTable.status]),
        createdAt = this[PetMembershipsTable.createdAt].toInstant(),
        updatedAt = this[PetMembershipsTable.updatedAt].toInstant(),
    )
}
