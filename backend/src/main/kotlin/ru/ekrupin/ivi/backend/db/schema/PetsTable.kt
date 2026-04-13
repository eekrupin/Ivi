package ru.ekrupin.ivi.backend.db.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PetsTable : UUIDTable("pets") {
    val name = varchar("name", 200)
    val birthDate = date("birth_date").nullable()
    val photoRevision = varchar("photo_revision", 160).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val version = long("version")
}
