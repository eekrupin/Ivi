package ru.ekrupin.ivi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val birthDate: LocalDate?,
    val photoUri: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
