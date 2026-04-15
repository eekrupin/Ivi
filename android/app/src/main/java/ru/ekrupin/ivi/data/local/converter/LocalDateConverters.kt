package ru.ekrupin.ivi.data.local.converter

import androidx.room.TypeConverter
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus
import java.time.LocalDate
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import ru.ekrupin.ivi.data.sync.model.SyncState

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

    @TypeConverter
    fun syncStateToString(value: SyncState?): String? = value?.name

    @TypeConverter
    fun stringToSyncState(value: String?): SyncState? = value?.let(SyncState::valueOf)

    @TypeConverter
    fun syncEntityTypeToString(value: SyncEntityType?): String? = value?.name

    @TypeConverter
    fun stringToSyncEntityType(value: String?): SyncEntityType? = value?.let(SyncEntityType::valueOf)

    @TypeConverter
    fun syncOperationToString(value: SyncOperation?): String? = value?.name

    @TypeConverter
    fun stringToSyncOperation(value: String?): SyncOperation? = value?.let(SyncOperation::valueOf)

    @TypeConverter
    fun syncOutboxStatusToString(value: SyncOutboxStatus?): String? = value?.name

    @TypeConverter
    fun stringToSyncOutboxStatus(value: String?): SyncOutboxStatus? = value?.let(SyncOutboxStatus::valueOf)
}
