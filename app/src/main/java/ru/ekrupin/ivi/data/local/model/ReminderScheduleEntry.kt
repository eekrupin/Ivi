package ru.ekrupin.ivi.data.local.model

import java.time.LocalDate
import ru.ekrupin.ivi.domain.model.PetEventStatus

data class ReminderScheduleEntry(
    val eventId: Long,
    val eventTypeName: String,
    val dueDate: LocalDate?,
    val notificationsEnabled: Boolean,
    val status: PetEventStatus,
)
