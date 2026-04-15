package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sync_users")
data class SyncUserEntity(
    @PrimaryKey val remoteId: String,
    val email: String,
    val displayName: String,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val lastSyncedAt: LocalDateTime,
)
