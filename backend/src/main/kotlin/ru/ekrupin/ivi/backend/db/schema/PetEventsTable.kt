package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PetEventsTable : UUIDTable("pet_events") {
    val petId = reference("pet_id", PetsTable, onDelete = ReferenceOption.CASCADE)
    val eventTypeId = reference("event_type_id", EventTypesTable, onDelete = ReferenceOption.RESTRICT)
    val eventDate = date("event_date")
    val dueDate = date("due_date").nullable()
    val comment = text("comment").nullable()
    val notificationsEnabled = bool("notifications_enabled")
    val status = varchar("status", 32)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val version = long("version")
}
