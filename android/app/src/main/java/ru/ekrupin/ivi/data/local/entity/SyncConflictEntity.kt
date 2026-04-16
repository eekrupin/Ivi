package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncEntityType

@Entity(
    tableName = "sync_conflicts",
    indices = [
        Index(value = ["entityType", "entityLocalId"], unique = true),
        Index("clientMutationId"),
    ],
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityLocalId: Long,
    val entityRemoteId: String,
    val clientMutationId: String,
    val reason: String,
    val serverVersion: Long,
    val serverRecordJson: String?,
    val conflictedAt: LocalDateTime,
)
