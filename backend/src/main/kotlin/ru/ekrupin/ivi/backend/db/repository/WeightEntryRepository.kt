package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.WeightEntryRecord
import ru.ekrupin.ivi.backend.db.schema.WeightEntriesTable
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class CreateWeightEntryCommand(
    val petId: UUID,
    val date: LocalDate,
    val weightGrams: Int,
    val comment: String?,
)

data class UpdateWeightEntryCommand(
    val date: LocalDate,
    val weightGrams: Int,
    val comment: String?,
)

class WeightEntryRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(command: CreateWeightEntryCommand, entityId: UUID = UUID.randomUUID()): WeightEntryRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = entityId

        databaseFactory.dbQuery {
            WeightEntriesTable.insert {
                it[WeightEntriesTable.id] = id
                it[petId] = command.petId
                it[date] = command.date
                it[weightGrams] = command.weightGrams
                it[comment] = command.comment
                it[createdAt] = now
                it[updatedAt] = now
                it[deletedAt] = null
                it[version] = 1L
            }
        }

        return findById(id) ?: error("Weight entry $id was not created")
    }

    fun findById(id: UUID): WeightEntryRecord? = databaseFactory.dbQueryResult {
        WeightEntriesTable.selectAll()
            .where { WeightEntriesTable.id eq id }
            .singleOrNull()
            ?.toWeightEntryRecord()
    }

    fun listByPetId(petId: UUID, includeDeleted: Boolean = false): List<WeightEntryRecord> = databaseFactory.dbQueryResult {
        WeightEntriesTable.selectAll()
            .where {
                if (includeDeleted) {
                    WeightEntriesTable.petId eq petId
                } else {
                    (WeightEntriesTable.petId eq petId) and WeightEntriesTable.deletedAt.isNull()
                }
            }
            .orderBy(WeightEntriesTable.date to SortOrder.DESC, WeightEntriesTable.createdAt to SortOrder.DESC)
            .map { it.toWeightEntryRecord() }
    }

    fun listChangedByPetId(petId: UUID, sinceExclusive: Instant, untilInclusive: Instant): List<WeightEntryRecord> {
        val since = sinceExclusive.atOffset(ZoneOffset.UTC)
        val until = untilInclusive.atOffset(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            WeightEntriesTable.selectAll()
                .where {
                    (WeightEntriesTable.petId eq petId) and
                        (WeightEntriesTable.updatedAt greater since) and
                        (WeightEntriesTable.updatedAt lessEq until)
                }
                .orderBy(WeightEntriesTable.updatedAt to SortOrder.ASC)
                .map { it.toWeightEntryRecord() }
        }
    }

    fun update(id: UUID, command: UpdateWeightEntryCommand): WeightEntryRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            WeightEntriesTable.update({ WeightEntriesTable.id eq id }) {
                it[date] = command.date
                it[weightGrams] = command.weightGrams
                it[comment] = command.comment
                it[deletedAt] = null
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    fun softDelete(id: UUID): WeightEntryRecord? {
        val current = findById(id) ?: return null
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        databaseFactory.dbQuery {
            WeightEntriesTable.update({ WeightEntriesTable.id eq id }) {
                it[deletedAt] = now
                it[updatedAt] = now
                it[version] = current.version + 1
            }
        }
        return findById(id)
    }

    private fun ResultRow.toWeightEntryRecord(): WeightEntryRecord = WeightEntryRecord(
        id = this[WeightEntriesTable.id].value,
        petId = this[WeightEntriesTable.petId].value,
        date = this[WeightEntriesTable.date],
        weightGrams = this[WeightEntriesTable.weightGrams],
        comment = this[WeightEntriesTable.comment],
        createdAt = this[WeightEntriesTable.createdAt].toInstant(),
        updatedAt = this[WeightEntriesTable.updatedAt].toInstant(),
        deletedAt = this[WeightEntriesTable.deletedAt]?.toInstant(),
        version = this[WeightEntriesTable.version],
    )
}
