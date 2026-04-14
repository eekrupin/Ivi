package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.EventCategoryEntity
import ru.ekrupin.ivi.backend.db.model.EventTypeRecord
import ru.ekrupin.ivi.backend.db.schema.EventTypesTable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class CreateEventTypeCommand(
    val petId: UUID,
    val name: String,
    val category: EventCategoryEntity,
    val defaultDurationDays: Int?,
    val isActive: Boolean,
    val colorArgb: Int?,
    val iconKey: String?,
)

data class UpdateEventTypeCommand(
    val name: String,
    val category: EventCategoryEntity,
    val defaultDurationDays: Int?,
    val isActive: Boolean,
    val colorArgb: Int?,
    val iconKey: String?,
)

class EventTypeRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(command: CreateEventTypeCommand): EventTypeRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            EventTypesTable.insert {
                it[EventTypesTable.id] = id
                it[petId] = command.petId
                it[name] = command.name
                it[category] = command.category.name
                it[defaultDurationDays] = command.defaultDurationDays
                it[isActive] = command.isActive
                it[colorArgb] = command.colorArgb
                it[iconKey] = command.iconKey
                it[createdAt] = now
                it[updatedAt] = now
                it[deletedAt] = null
                it[version] = 1L
            }
        }

        return findById(id) ?: error("Event type $id was not created")
    }

    fun findById(id: UUID): EventTypeRecord? = databaseFactory.dbQueryResult {
        EventTypesTable.selectAll()
            .where { EventTypesTable.id eq id }
            .singleOrNull()
            ?.toEventTypeRecord()
    }

    fun listByPetId(petId: UUID, includeDeleted: Boolean = false): List<EventTypeRecord> = databaseFactory.dbQueryResult {
        EventTypesTable.selectAll()
            .where {
                if (includeDeleted) {
                    EventTypesTable.petId eq petId
                } else {
                    (EventTypesTable.petId eq petId) and EventTypesTable.deletedAt.isNull()
                }
            }
            .orderBy(EventTypesTable.createdAt to SortOrder.ASC)
            .map { it.toEventTypeRecord() }
    }

    fun listChangedByPetId(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<EventTypeRecord> {
        val since = sinceExclusive.atOffset(ZoneOffset.UTC)
        val until = untilInclusive.atOffset(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            EventTypesTable.selectAll()
                .where {
                    (EventTypesTable.petId eq petId) and
                        (EventTypesTable.updatedAt greater since) and
                        (EventTypesTable.updatedAt lessEq until)
                }
                .orderBy(EventTypesTable.updatedAt to SortOrder.ASC)
                .map { it.toEventTypeRecord() }
        }
    }

    fun update(id: UUID, command: UpdateEventTypeCommand): EventTypeRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            EventTypesTable.update({ EventTypesTable.id eq id }) {
                it[name] = command.name
                it[category] = command.category.name
                it[defaultDurationDays] = command.defaultDurationDays
                it[isActive] = command.isActive
                it[colorArgb] = command.colorArgb
                it[iconKey] = command.iconKey
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    fun softDelete(id: UUID): EventTypeRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            EventTypesTable.update({ EventTypesTable.id eq id }) {
                it[deletedAt] = now
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    private fun ResultRow.toEventTypeRecord(): EventTypeRecord = EventTypeRecord(
        id = this[EventTypesTable.id].value,
        petId = this[EventTypesTable.petId].value,
        name = this[EventTypesTable.name],
        category = EventCategoryEntity.valueOf(this[EventTypesTable.category]),
        defaultDurationDays = this[EventTypesTable.defaultDurationDays],
        isActive = this[EventTypesTable.isActive],
        colorArgb = this[EventTypesTable.colorArgb],
        iconKey = this[EventTypesTable.iconKey],
        createdAt = this[EventTypesTable.createdAt].toInstant(),
        updatedAt = this[EventTypesTable.updatedAt].toInstant(),
        deletedAt = this[EventTypesTable.deletedAt]?.toInstant(),
        version = this[EventTypesTable.version],
    )
}
