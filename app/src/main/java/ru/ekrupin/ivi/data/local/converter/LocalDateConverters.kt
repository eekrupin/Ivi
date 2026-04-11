package ru.ekrupin.ivi.data.local.converter

import androidx.room.TypeConverter
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus
import java.time.LocalDate
import java.time.LocalDateTime

class LocalDateConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun eventCategoryToString(value: EventCategory?): String? = value?.name

    @TypeConverter
    fun stringToEventCategory(value: String?): EventCategory? = value?.let(EventCategory::valueOf)

    @TypeConverter
    fun petEventStatusToString(value: PetEventStatus?): String? = value?.name

    @TypeConverter
    fun stringToPetEventStatus(value: String?): PetEventStatus? = value?.let(PetEventStatus::valueOf)
}
