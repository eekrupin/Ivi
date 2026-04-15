package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.ekrupin.ivi.domain.model.PetEventStatus
import java.time.LocalDate
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncState

@Entity(
    tableName = "pet_events",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EventTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventTypeId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("petId"), Index("eventTypeId"), Index("eventDate")],
)
data class PetEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val eventTypeId: Long,
    val eventDate: LocalDate,
    val dueDate: LocalDate?,
    val comment: String?,
    val notificationsEnabled: Boolean,
    val status: PetEventStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remoteId: String? = null,
    val serverVersion: Long? = null,
    val serverUpdatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncedAt: LocalDateTime? = null,
)
