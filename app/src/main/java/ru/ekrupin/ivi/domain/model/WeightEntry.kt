package ru.ekrupin.ivi.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class WeightEntry(
    val id: Long,
    val petId: Long,
    val date: LocalDate,
    val weightGrams: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
)
