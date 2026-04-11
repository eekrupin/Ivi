package ru.ekrupin.ivi.data.mapper

import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.ReminderSettingsEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.domain.model.EventType
import ru.ekrupin.ivi.domain.model.Pet
import ru.ekrupin.ivi.domain.model.PetEvent
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.model.WeightEntry

fun PetEntity.toDomain(): Pet = Pet(
    id = id,
    name = name,
    birthDate = birthDate,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Pet.toEntity(): PetEntity = PetEntity(
    id = id,
    name = name,
    birthDate = birthDate,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WeightEntryEntity.toDomain(): WeightEntry = WeightEntry(
    id = id,
    petId = petId,
    date = date,
    weightGrams = weightGrams,
    comment = comment,
    createdAt = createdAt,
)

fun EventTypeEntity.toDomain(): EventType = EventType(
    id = id,
    name = name,
    category = category,
    defaultDurationDays = defaultDurationDays,
    isActive = isActive,
    colorArgb = colorArgb,
    iconKey = iconKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun EventType.toEntity(): EventTypeEntity = EventTypeEntity(
    id = id,
    name = name,
    category = category,
    defaultDurationDays = defaultDurationDays,
    isActive = isActive,
    colorArgb = colorArgb,
    iconKey = iconKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PetEventEntity.toDomain(): PetEvent = PetEvent(
    id = id,
    petId = petId,
    eventTypeId = eventTypeId,
    eventDate = eventDate,
    dueDate = dueDate,
    comment = comment,
    notificationsEnabled = notificationsEnabled,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PetEvent.toEntity(): PetEventEntity = PetEventEntity(
    id = id,
    petId = petId,
    eventTypeId = eventTypeId,
    eventDate = eventDate,
    dueDate = dueDate,
    comment = comment,
    notificationsEnabled = notificationsEnabled,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderSettingsEntity.toDomain(): ReminderSettings = ReminderSettings(
    id = id,
    firstReminderEnabled = firstReminderEnabled,
    firstReminderDaysBefore = firstReminderDaysBefore,
    secondReminderEnabled = secondReminderEnabled,
    secondReminderDaysBefore = secondReminderDaysBefore,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderSettings.toEntity(): ReminderSettingsEntity = ReminderSettingsEntity(
    id = id,
    firstReminderEnabled = firstReminderEnabled,
    firstReminderDaysBefore = firstReminderDaysBefore,
    secondReminderEnabled = secondReminderEnabled,
    secondReminderDaysBefore = secondReminderDaysBefore,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
