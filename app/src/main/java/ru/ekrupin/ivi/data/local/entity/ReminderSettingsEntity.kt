package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val firstReminderEnabled: Boolean,
    val firstReminderDaysBefore: Int,
    val secondReminderEnabled: Boolean,
    val secondReminderDaysBefore: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
