package ru.ekrupin.ivi.data.local.seed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.ReminderSettingsDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.ReminderSettingsEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus

@Singleton
class DatabaseSeeder @Inject constructor(
    private val petDao: PetDao,
    private val weightEntryDao: WeightEntryDao,
    private val eventTypeDao: EventTypeDao,
    private val petEventDao: PetEventDao,
    private val reminderSettingsDao: ReminderSettingsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfNeeded() {
        scope.launch {
            if (petDao.count() > 0) return@launch

            val now = LocalDateTime.now()
            val today = LocalDate.now()

            petDao.insert(
                PetEntity(
                    id = AppConstants.PET_ID,
                    name = "Иви",
                    birthDate = today.minusYears(3).minusMonths(2),
                    photoUri = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            eventTypeDao.insertAll(
                listOf(
                    EventTypeEntity(
                        id = 1,
                        name = "Бравекто 3",
                        category = EventCategory.TICK_PROTECTION,
                        defaultDurationDays = 84,
                        isActive = true,
                        colorArgb = 0xFFC98656,
                        iconKey = "shield",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    EventTypeEntity(
                        id = 2,
                        name = "Симпарика",
                        category = EventCategory.TICK_PROTECTION,
                        defaultDurationDays = 30,
                        isActive = true,
                        colorArgb = 0xFFB85C38,
                        iconKey = "pill",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    EventTypeEntity(
                        id = 3,
                        name = "Прививка",
                        category = EventCategory.VACCINATION,
                        defaultDurationDays = 365,
                        isActive = true,
                        colorArgb = 0xFF8E5A3C,
                        iconKey = "syringe",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    EventTypeEntity(
                        id = 4,
                        name = "Глистогонное",
                        category = EventCategory.DEWORMING,
                        defaultDurationDays = 90,
                        isActive = true,
                        colorArgb = 0xFFD29A6C,
                        iconKey = "medical",
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )

            weightEntryDao.insertAll(
                listOf(
                    WeightEntryEntity(
                        petId = AppConstants.PET_ID,
                        date = today.minusMonths(2),
                        weightGrams = 7900,
                        comment = "После зимы",
                        createdAt = now,
                    ),
                    WeightEntryEntity(
                        petId = AppConstants.PET_ID,
                        date = today.minusMonths(1),
                        weightGrams = 8100,
                        comment = "Новый замер дома",
                        createdAt = now,
                    ),
                    WeightEntryEntity(
                        petId = AppConstants.PET_ID,
                        date = today.minusDays(10),
                        weightGrams = 8400,
                        comment = "Перед обработкой",
                        createdAt = now,
                    ),
                ),
            )

            petEventDao.insertAll(
                listOf(
                    PetEventEntity(
                        petId = AppConstants.PET_ID,
                        eventTypeId = 2,
                        eventDate = today.minusDays(8),
                        dueDate = today.plusDays(22),
                        comment = "Ежемесячная защита",
                        notificationsEnabled = true,
                        status = PetEventStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    PetEventEntity(
                        petId = AppConstants.PET_ID,
                        eventTypeId = 4,
                        eventDate = today.minusDays(30),
                        dueDate = today.plusDays(60),
                        comment = "Весной",
                        notificationsEnabled = true,
                        status = PetEventStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    PetEventEntity(
                        petId = AppConstants.PET_ID,
                        eventTypeId = 3,
                        eventDate = today.minusMonths(4),
                        dueDate = today.plusMonths(8),
                        comment = "Плановая ежегодная прививка",
                        notificationsEnabled = false,
                        status = PetEventStatus.COMPLETED,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )

            reminderSettingsDao.insert(
                ReminderSettingsEntity(
                    id = AppConstants.REMINDER_SETTINGS_ID,
                    firstReminderEnabled = true,
                    firstReminderDaysBefore = 7,
                    secondReminderEnabled = true,
                    secondReminderDaysBefore = 2,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
