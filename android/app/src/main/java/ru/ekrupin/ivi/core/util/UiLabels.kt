package ru.ekrupin.ivi.core.util

import androidx.annotation.StringRes
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus

@StringRes
fun EventCategory.labelRes(): Int = when (this) {
    EventCategory.TICK_PROTECTION -> R.string.category_tick_protection
    EventCategory.DEWORMING -> R.string.category_deworming
    EventCategory.VACCINATION -> R.string.category_vaccination
    EventCategory.CHECKUP -> R.string.category_checkup
    EventCategory.OTHER -> R.string.category_other
}

@StringRes
fun PetEventStatus.labelRes(): Int = when (this) {
    PetEventStatus.ACTIVE -> R.string.events_filter_active
    PetEventStatus.COMPLETED -> R.string.events_filter_completed
    PetEventStatus.ARCHIVED -> R.string.events_filter_archived
}
