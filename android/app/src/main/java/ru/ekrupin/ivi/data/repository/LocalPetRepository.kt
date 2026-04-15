package ru.ekrupin.ivi.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import androidx.room.withTransaction
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.mapper.toDomain
import ru.ekrupin.ivi.data.sync.model.SyncState
import ru.ekrupin.ivi.domain.model.Pet
import ru.ekrupin.ivi.domain.repository.PetRepository

class LocalPetRepository @Inject constructor(
    private val database: IviDatabase,
    private val petDao: PetDao,
) : PetRepository {
    override fun observePet(): Flow<Pet?> = petDao.observePet().map { it?.toDomain() }

    override suspend fun savePet(name: String, birthDate: LocalDate?, photoUri: String?) {
        val current = petDao.observePet()
        val now = LocalDateTime.now()
        val existing = current.first()
        val pet = PetEntity(
            id = existing?.id ?: 1L,
            name = name,
            birthDate = birthDate,
            photoUri = photoUri?.takeIf { it.isNotBlank() },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            remoteId = existing?.remoteId ?: UUID.randomUUID().toString(),
            serverVersion = existing?.serverVersion,
            serverUpdatedAt = existing?.serverUpdatedAt,
            deletedAt = null,
            syncState = existing?.syncState ?: SyncState.SYNCED,
            lastSyncedAt = existing?.lastSyncedAt,
        )
        database.withTransaction {
            petDao.insert(pet)
        }
    }
}
