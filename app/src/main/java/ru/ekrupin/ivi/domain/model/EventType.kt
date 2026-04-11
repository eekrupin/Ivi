package ru.ekrupin.ivi.domain.model

import java.time.LocalDateTime

data class EventType(
    val id: Long,
    val name: String,
    val category: EventCategory,
    val defaultDurationDays: Int?,
    val isActive: Boolean,
    val colorArgb: Long?,
    val iconKey: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
