package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.UserRecord
import ru.ekrupin.ivi.backend.db.schema.UsersTable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(email: String, passwordHash: String, displayName: String): UserRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.displayName] = displayName
                it[UsersTable.createdAt] = now
                it[UsersTable.updatedAt] = now
            }
        }

        return findById(id) ?: error("User $id was not created")
    }

    fun findById(id: UUID): UserRecord? = databaseFactory.dbQueryResult {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUserRecord()
    }

    fun findByEmail(email: String): UserRecord? = databaseFactory.dbQueryResult {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUserRecord()
    }

    private fun ResultRow.toUserRecord(): UserRecord = UserRecord(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        displayName = this[UsersTable.displayName],
        createdAt = this[UsersTable.createdAt].toInstant(),
        updatedAt = this[UsersTable.updatedAt].toInstant(),
    )
}
