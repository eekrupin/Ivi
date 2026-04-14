package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object EventTypesTable : UUIDTable("event_types") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 200)
    val category = varchar("category", 32)
    val defaultDurationDays = integer("default_duration_days").nullable()
    val isActive = bool("is_active")
    val colorArgb = integer("color_argb").nullable()
    val iconKey = varchar("icon_key", 100).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val version = long("version")
}
