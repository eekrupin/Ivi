package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PetPhotosTable : Table("pet_photos") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val revision = varchar("revision", 160)
    val contentType = varchar("content_type", 64)
    val data = binary("data")
    val sizeBytes = integer("size_bytes")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(petId)
}
