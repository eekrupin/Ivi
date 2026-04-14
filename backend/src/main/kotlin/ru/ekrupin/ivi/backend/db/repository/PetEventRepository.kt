package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.PetEventRecord
import ru.ekrupin.ivi.backend.db.model.PetEventStatusEntity
import ru.ekrupin.ivi.backend.db.schema.PetEventsTable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class CreatePetEventCommand(
    val petId: UUID,
    val eventTypeId: UUID,
    val eventDate: LocalDate,
    val dueDate: LocalDate?,
    val comment: String?,
    val notificationsEnabled: Boolean,
    val status: PetEventStatusEntity,
)

data class UpdatePetEventCommand(
    val eventTypeId: UUID,
    val eventDate: LocalDate,
    val dueDate: LocalDate?,
    val comment: String?,
    val notificationsEnabled: Boolean,
    val status: PetEventStatusEntity,
)

class PetEventRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(command: CreatePetEventCommand): PetEventRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            PetEventsTable.insert {
                it[PetEventsTable.id] = id
                it[petId] = command.petId
                it[eventTypeId] = command.eventTypeId
                it[eventDate] = command.eventDate
                it[dueDate] = command.dueDate
                it[comment] = command.comment
                it[notificationsEnabled] = command.notificationsEnabled
                it[status] = command.status.name
                it[createdAt] = now
                it[updatedAt] = now
                it[deletedAt] = null
                it[version] = 1L
            }
        }

        return findById(id) ?: error("Pet event $id was not created")
    }

    fun findById(id: UUID): PetEventRecord? = databaseFactory.dbQueryResult {
        PetEventsTable.selectAll()
            .where { PetEventsTable.id eq id }
            .singleOrNull()
            ?.toPetEventRecord()
    }

    fun listByPetId(petId: UUID, includeDeleted: Boolean = false): List<PetEventRecord> = databaseFactory.dbQueryResult {
        PetEventsTable.selectAll()
            .where {
                if (includeDeleted) {
                    PetEventsTable.petId eq petId
                } else {
                    (PetEventsTable.petId eq petId) and PetEventsTable.deletedAt.isNull()
                }
            }
            .orderBy(PetEventsTable.eventDate to SortOrder.DESC, PetEventsTable.createdAt to SortOrder.DESC)
            .map { it.toPetEventRecord() }
    }

    fun update(id: UUID, command: UpdatePetEventCommand): PetEventRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            PetEventsTable.update({ PetEventsTable.id eq id }) {
                it[eventTypeId] = command.eventTypeId
                it[eventDate] = command.eventDate
                it[dueDate] = command.dueDate
                it[comment] = command.comment
                it[notificationsEnabled] = command.notificationsEnabled
                it[status] = command.status.name
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    fun softDelete(id: UUID): PetEventRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            PetEventsTable.update({ PetEventsTable.id eq id }) {
                it[deletedAt] = now
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    private fun ResultRow.toPetEventRecord(): PetEventRecord = PetEventRecord(
        id = this[PetEventsTable.id].value,
        petId = this[PetEventsTable.petId].value,
        eventTypeId = this[PetEventsTable.eventTypeId].value,
        eventDate = this[PetEventsTable.eventDate],
        dueDate = this[PetEventsTable.dueDate],
        comment = this[PetEventsTable.comment],
        notificationsEnabled = this[PetEventsTable.notificationsEnabled],
        status = PetEventStatusEntity.valueOf(this[PetEventsTable.status]),
        createdAt = this[PetEventsTable.createdAt].toInstant(),
        updatedAt = this[PetEventsTable.updatedAt].toInstant(),
        deletedAt = this[PetEventsTable.deletedAt]?.toInstant(),
        version = this[PetEventsTable.version],
    )
}
