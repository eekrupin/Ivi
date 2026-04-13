package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.InviteRecord
import ru.ekrupin.ivi.backend.db.model.InviteStatusEntity
import ru.ekrupin.ivi.backend.db.schema.InvitesTable
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
}
