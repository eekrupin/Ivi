package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 320).uniqueIndex()
    val passwordHash = text("password_hash")
    val displayName = varchar("display_name", 200)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
