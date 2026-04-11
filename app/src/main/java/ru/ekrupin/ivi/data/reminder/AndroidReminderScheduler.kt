package ru.ekrupin.ivi.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.ReminderSettingsDao
import ru.ekrupin.ivi.domain.model.PetEventStatus

@Singleton
class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val petEventDao: PetEventDao,
    private val reminderSettingsDao: ReminderSettingsDao,
) : ReminderScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val sharedPreferences = context.getSharedPreferences(ReminderConstants.sharedPreferencesName, Context.MODE_PRIVATE)

    override fun refreshAll() {
        scope.launch {
            context.ensureReminderNotificationChannel()
            rebuildSchedule()
        }
    }

    private suspend fun rebuildSchedule() {
        cancelStoredReminders()

        val settings = reminderSettingsDao.get() ?: return
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val keys = mutableSetOf<String>()

        petEventDao.getReminderEntries().forEach { entry ->
            val dueDate = entry.dueDate ?: return@forEach
            if (!entry.notificationsEnabled) return@forEach
            if (entry.status == PetEventStatus.ARCHIVED) return@forEach

            scheduleIfNeeded(
                key = reminderKey(entry.eventId, 1),
                eventTypeName = entry.eventTypeName,
                dueDate = dueDate,
                reminderDate = if (settings.firstReminderEnabled) dueDate.minusDays(settings.firstReminderDaysBefore.toLong()) else null,
                now = now,
                zoneId = zoneId,
                keys = keys,
            )
            scheduleIfNeeded(
                key = reminderKey(entry.eventId, 2),
                eventTypeName = entry.eventTypeName,
                dueDate = dueDate,
                reminderDate = if (settings.secondReminderEnabled) dueDate.minusDays(settings.secondReminderDaysBefore.toLong()) else null,
                now = now,
                zoneId = zoneId,
                keys = keys,
            )
        }

        sharedPreferences.edit().putStringSet(ReminderConstants.scheduledKeys, keys).apply()
    }

    private fun scheduleIfNeeded(
        key: String,
        eventTypeName: String,
        dueDate: LocalDate,
        reminderDate: LocalDate?,
        now: Instant,
        zoneId: ZoneId,
        keys: MutableSet<String>,
    ) {
        val date = reminderDate ?: return
        val triggerAt = date.atTime(ReminderConstants.notificationHour, ReminderConstants.notificationMinute)
            .atZone(zoneId)
            .toInstant()
        if (triggerAt <= now) return

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toEpochMilli(),
            buildPendingIntent(
                requestCode = key.hashCode(),
                title = context.getString(R.string.reminder_notification_title),
                text = context.getString(
                    R.string.reminder_notification_text,
                    eventTypeName,
                    dueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                ),
            ),
        )
        keys += key
    }

    private fun cancelStoredReminders() {
        val keys = sharedPreferences.getStringSet(ReminderConstants.scheduledKeys, emptySet()).orEmpty()
        keys.forEach { key ->
            alarmManager.cancel(buildPendingIntent(key.hashCode(), "", ""))
        }
        sharedPreferences.edit().remove(ReminderConstants.scheduledKeys).apply()
    }

    private fun buildPendingIntent(
        requestCode: Int,
        title: String,
        text: String,
    ): PendingIntent {
        val intent = Intent(context, ReminderPublisherReceiver::class.java)
            .putExtra(ReminderConstants.extraNotificationId, requestCode)
            .putExtra(ReminderConstants.extraNotificationTitle, title)
            .putExtra(ReminderConstants.extraNotificationText, text)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderKey(eventId: Long, slot: Int): String = "$eventId-$slot"
}
