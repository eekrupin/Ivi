package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 128).uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
