package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.RefreshTokenRecord
import ru.ekrupin.ivi.backend.db.schema.RefreshTokensTable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class RefreshTokenRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(userId: UUID, tokenHash: String, expiresAtEpochMillis: Long): RefreshTokenRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()
        val expiresAt = OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(expiresAtEpochMillis), ZoneOffset.UTC)

        databaseFactory.dbQuery {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.id] = id
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[RefreshTokensTable.revokedAt] = null
                it[RefreshTokensTable.createdAt] = now
            }
        }

        return findActiveByHash(tokenHash) ?: error("Refresh token $id was not created")
    }

    fun findActiveByHash(tokenHash: String): RefreshTokenRecord? = databaseFactory.dbQueryResult {
        RefreshTokensTable.selectAll()
            .where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    RefreshTokensTable.revokedAt.isNull()
            }
            .singleOrNull()
            ?.toRefreshTokenRecord()
    }

    fun revoke(tokenHash: String) {
        val revokedAt = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
                it[RefreshTokensTable.revokedAt] = revokedAt
            }
        }
    }

    private fun ResultRow.toRefreshTokenRecord(): RefreshTokenRecord = RefreshTokenRecord(
        id = this[RefreshTokensTable.id].value,
        userId = this[RefreshTokensTable.userId].value,
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt].toInstant(),
        revokedAt = this[RefreshTokensTable.revokedAt]?.toInstant(),
        createdAt = this[RefreshTokensTable.createdAt].toInstant(),
    )
}
