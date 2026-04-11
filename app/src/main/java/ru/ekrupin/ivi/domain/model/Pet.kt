package ru.ekrupin.ivi.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Pet(
    val id: Long,
    val name: String,
    val birthDate: LocalDate?,
    val photoUri: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
