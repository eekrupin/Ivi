package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.PetRecord
import ru.ekrupin.ivi.backend.db.schema.PetsTable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PetRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun create(name: String, birthDate: LocalDate?): PetRecord {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        databaseFactory.dbQuery {
            PetsTable.insert {
                it[PetsTable.id] = id
                it[PetsTable.name] = name
                it[PetsTable.birthDate] = birthDate
                it[PetsTable.photoRevision] = null
                it[PetsTable.createdAt] = now
                it[PetsTable.updatedAt] = now
                it[PetsTable.deletedAt] = null
                it[PetsTable.version] = 1L
            }
        }

        return findById(id) ?: error("Pet $id was not created")
    }

    fun findById(id: UUID): PetRecord? = databaseFactory.dbQueryResult {
        PetsTable.selectAll()
            .where { PetsTable.id eq id }
            .singleOrNull()
            ?.toPetRecord()
    }

    private fun ResultRow.toPetRecord(): PetRecord = PetRecord(
        id = this[PetsTable.id].value,
        name = this[PetsTable.name],
        birthDate = this[PetsTable.birthDate],
        photoRevision = this[PetsTable.photoRevision],
        createdAt = this[PetsTable.createdAt].toInstant(),
        updatedAt = this[PetsTable.updatedAt].toInstant(),
        deletedAt = this[PetsTable.deletedAt]?.toInstant(),
        version = this[PetsTable.version],
    )
}
