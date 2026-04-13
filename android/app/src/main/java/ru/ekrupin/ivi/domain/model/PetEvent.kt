package ru.ekrupin.ivi.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class PetEvent(
    val id: Long,
    val petId: Long,
    val eventTypeId: Long,
    val eventDate: LocalDate,
    val dueDate: LocalDate?,
    val comment: String?,
    val notificationsEnabled: Boolean,
    val status: PetEventStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
