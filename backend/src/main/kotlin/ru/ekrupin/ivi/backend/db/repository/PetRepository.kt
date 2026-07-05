package ru.ekrupin.ivi.backend.db.repository

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.model.MembershipRoleEntity
import ru.ekrupin.ivi.backend.db.model.MembershipStatusEntity
import ru.ekrupin.ivi.backend.db.model.PetRecord
import ru.ekrupin.ivi.backend.db.schema.PetMembershipsTable
import ru.ekrupin.ivi.backend.db.schema.PetsTable
import java.time.Instant
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

    fun createWithOwnerMembership(name: String, birthDate: LocalDate?, ownerUserId: UUID): CreatePetWithOwnerResult {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val petId = UUID.randomUUID()
        val membershipId = UUID.randomUUID()

        return try {
            databaseFactory.dbQueryResult {
                val hasActiveMembership = PetMembershipsTable.selectAll()
                    .where {
                        (PetMembershipsTable.userId eq ownerUserId) and
                            (PetMembershipsTable.status eq MembershipStatusEntity.ACTIVE.name)
                    }
                    .limit(1)
                    .any()

                if (hasActiveMembership) {
                    return@dbQueryResult CreatePetWithOwnerResult.AlreadyBound
                }

                PetsTable.insert {
                    it[PetsTable.id] = petId
                    it[PetsTable.name] = name
                    it[PetsTable.birthDate] = birthDate
                    it[PetsTable.photoRevision] = null
                    it[PetsTable.createdAt] = now
                    it[PetsTable.updatedAt] = now
                    it[PetsTable.deletedAt] = null
                    it[PetsTable.version] = 1L
                }

                PetMembershipsTable.insert {
                    it[PetMembershipsTable.id] = membershipId
                    it[PetMembershipsTable.petId] = petId
                    it[PetMembershipsTable.userId] = ownerUserId
                    it[PetMembershipsTable.role] = MembershipRoleEntity.OWNER.name
                    it[PetMembershipsTable.status] = MembershipStatusEntity.ACTIVE.name
                    it[PetMembershipsTable.createdAt] = now
                    it[PetMembershipsTable.updatedAt] = now
                }

                val pet = PetsTable.selectAll()
                    .where { PetsTable.id eq petId }
                    .single()
                    .toPetRecord()

                CreatePetWithOwnerResult.Created(pet)
            }
        } catch (exception: ExposedSQLException) {
            if (exception.isActiveMembershipUniqueViolation()) {
                CreatePetWithOwnerResult.AlreadyBound
            } else {
                throw exception
            }
        }
    }

    fun findById(id: UUID): PetRecord? = databaseFactory.dbQueryResult {
        PetsTable.selectAll()
            .where { PetsTable.id eq id }
            .singleOrNull()
            ?.toPetRecord()
    }

    fun listChangedByIds(ids: Collection<UUID>, sinceExclusive: Instant, untilInclusive: Instant): List<PetRecord> {
        if (ids.isEmpty()) return emptyList()
        val since = sinceExclusive.atOffset(ZoneOffset.UTC)
        val until = untilInclusive.atOffset(ZoneOffset.UTC)
        return databaseFactory.dbQueryResult {
            PetsTable.selectAll()
                .where {
                    (PetsTable.id inList ids) and
                        (PetsTable.updatedAt greater since) and
                        (PetsTable.updatedAt lessEq until)
                }
                .map { it.toPetRecord() }
        }
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

    sealed interface CreatePetWithOwnerResult {
        data class Created(val pet: PetRecord) : CreatePetWithOwnerResult

        data object AlreadyBound : CreatePetWithOwnerResult
    }

    private fun ExposedSQLException.isActiveMembershipUniqueViolation(): Boolean {
        return sqlState == POSTGRES_UNIQUE_VIOLATION &&
            generateSequence<Throwable>(this) { it.cause }
                .any { it.message?.contains(ACTIVE_MEMBERSHIP_UNIQUE_INDEX) == true }
    }

    private companion object {
        const val POSTGRES_UNIQUE_VIOLATION = "23505"
        const val ACTIVE_MEMBERSHIP_UNIQUE_INDEX = "uq_pet_memberships_user_id_active"
    }
}
