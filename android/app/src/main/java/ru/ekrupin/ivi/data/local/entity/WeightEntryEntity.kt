package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncState

@Entity(
    tableName = "weight_entries",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("petId"), Index("date")],
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val date: LocalDate,
    val weightGrams: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime = createdAt,
    val remoteId: String? = null,
    val serverVersion: Long? = null,
    val serverUpdatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncedAt: LocalDateTime? = null,
)
