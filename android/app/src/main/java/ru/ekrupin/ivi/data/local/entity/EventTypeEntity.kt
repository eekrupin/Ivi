package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.ekrupin.ivi.domain.model.EventCategory
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncState

@Entity(tableName = "event_types")
data class EventTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: EventCategory,
    val defaultDurationDays: Int?,
    val isActive: Boolean,
    val colorArgb: Long?,
    val iconKey: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remoteId: String? = null,
    val serverVersion: Long? = null,
    val serverUpdatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncedAt: LocalDateTime? = null,
)
