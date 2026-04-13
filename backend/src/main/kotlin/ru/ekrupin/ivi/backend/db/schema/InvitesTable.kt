package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object InvitesTable : UUIDTable("invites") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val createdByUserId = reference("created_by_user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val code = varchar("code", 128).uniqueIndex()
    val status = varchar("status", 32)
    val expiresAt = timestampWithTimeZone("expires_at")
    val acceptedByUserId = reference("accepted_by_user_id", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val acceptedAt = timestampWithTimeZone("accepted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
