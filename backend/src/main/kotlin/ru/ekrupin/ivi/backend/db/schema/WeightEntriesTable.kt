package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object WeightEntriesTable : UUIDTable("weight_entries") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val weightGrams = integer("weight_grams")
    val comment = text("comment").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val version = long("version")
}
