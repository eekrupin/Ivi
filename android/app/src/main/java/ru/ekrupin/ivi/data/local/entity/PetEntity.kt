package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncState

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val birthDate: LocalDate?,
    val photoUri: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remoteId: String? = null,
    val serverVersion: Long? = null,
    val serverUpdatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncedAt: LocalDateTime? = null,
)
