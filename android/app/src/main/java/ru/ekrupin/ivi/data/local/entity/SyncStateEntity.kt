package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val cursor: String?,
    val lastBootstrapAt: LocalDateTime?,
    val lastChangesAt: LocalDateTime?,
    val lastSuccessfulReadAt: LocalDateTime?,
    val requiresBootstrap: Boolean,
)
