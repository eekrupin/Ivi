package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PetMembershipsTable : UUIDTable("pet_memberships") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 32)
    val status = varchar("status", 32)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    init {
        uniqueIndex("uq_pet_memberships_pet_id_user_id", petId, userId)
    }
}
