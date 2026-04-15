package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index("status"),
        Index("entityType", "entityLocalId"),
        Index("entityRemoteId"),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityLocalId: Long,
    val entityRemoteId: String,
    val operation: SyncOperation,
    val payloadJson: String?,
    val baseVersion: Long?,
    val clientMutationId: String,
    val status: SyncOutboxStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
