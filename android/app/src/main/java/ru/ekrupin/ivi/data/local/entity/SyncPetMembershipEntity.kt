package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sync_pet_memberships")
data class SyncPetMembershipEntity(
    @PrimaryKey val remoteId: String,
    val petRemoteId: String,
    val userRemoteId: String,
    val role: String,
    val status: String,
    val serverVersion: Long,
    val serverUpdatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val lastSyncedAt: LocalDateTime,
)
