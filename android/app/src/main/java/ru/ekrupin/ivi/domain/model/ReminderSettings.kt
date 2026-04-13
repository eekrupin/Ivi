package ru.ekrupin.ivi.domain.model

import java.time.LocalDateTime

data class ReminderSettings(
    val id: Long,
    val firstReminderEnabled: Boolean,
    val firstReminderDaysBefore: Int,
    val secondReminderEnabled: Boolean,
    val secondReminderDaysBefore: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
