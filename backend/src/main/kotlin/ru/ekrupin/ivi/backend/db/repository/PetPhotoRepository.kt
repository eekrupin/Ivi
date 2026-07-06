package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.PetPhotoRecord
import ru.ekrupin.ivi.backend.db.schema.PetPhotosTable
import ru.ekrupin.ivi.backend.db.schema.PetsTable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PetPhotoRepository(
    private val databaseFactory: DatabaseFactory,
) {
    fun upsert(petId: UUID, revision: String, contentType: String, data: ByteArray): PetPhotoWriteResult? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            val pet = PetsTable.selectAll()
                .where {
                    (PetsTable.id eq petId) and PetsTable.deletedAt.isNull()
                }
                .forUpdate()
                .singleOrNull()
                ?: return@dbQueryResult null

            val hadPhoto = PetPhotosTable.selectAll()
                .where { PetPhotosTable.petId eq petId }
                .forUpdate()
                .any()

            if (hadPhoto) {
                PetPhotosTable.update({ PetPhotosTable.petId eq petId }) {
                    it[PetPhotosTable.revision] = revision
                    it[PetPhotosTable.contentType] = contentType
                    it[PetPhotosTable.data] = data
                    it[PetPhotosTable.sizeBytes] = data.size
                    it[PetPhotosTable.updatedAt] = now
                }
            } else {
                PetPhotosTable.insert {
                    it[PetPhotosTable.petId] = petId
                    it[PetPhotosTable.revision] = revision
                    it[PetPhotosTable.contentType] = contentType
                    it[PetPhotosTable.data] = data
                    it[PetPhotosTable.sizeBytes] = data.size
                    it[PetPhotosTable.updatedAt] = now
                }
            }

            PetsTable.update({ (PetsTable.id eq petId) and PetsTable.deletedAt.isNull() }) {
                it[PetsTable.photoRevision] = revision
                it[PetsTable.updatedAt] = now
                it[PetsTable.version] = pet[PetsTable.version] + 1
            }

            PetPhotoWriteResult(revision = revision, replaced = hadPhoto)
        }
    }

    fun delete(petId: UUID): Boolean? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            val pet = PetsTable.selectAll()
                .where {
                    (PetsTable.id eq petId) and PetsTable.deletedAt.isNull()
                }
                .forUpdate()
                .singleOrNull()
                ?: return@dbQueryResult null

            PetPhotosTable.deleteWhere { PetPhotosTable.petId eq petId }
            PetsTable.update({ (PetsTable.id eq petId) and PetsTable.deletedAt.isNull() }) {
                it[PetsTable.photoRevision] = null
                it[PetsTable.updatedAt] = now
                it[PetsTable.version] = pet[PetsTable.version] + 1
            }
            true
        }
    }

    fun find(petId: UUID): PetPhotoRecord? = databaseFactory.dbQueryResult {
        PetPhotosTable.selectAll()
            .where { PetPhotosTable.petId eq petId }
            .singleOrNull()
            ?.toPetPhotoRecord()
    }

    private fun ResultRow.toPetPhotoRecord(): PetPhotoRecord = PetPhotoRecord(
        petId = this[PetPhotosTable.petId].value,
        revision = this[PetPhotosTable.revision],
        contentType = this[PetPhotosTable.contentType],
        data = this[PetPhotosTable.data],
        sizeBytes = this[PetPhotosTable.sizeBytes],
        updatedAt = this[PetPhotosTable.updatedAt].toInstant(),
    )
}

data class PetPhotoWriteResult(
    val revision: String,
    val replaced: Boolean,
)
