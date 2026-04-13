package ru.ekrupin.ivi.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ru.ekrupin.ivi.R

fun Context.ensureReminderNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val notificationManager = getSystemService(NotificationManager::class.java)
    val existing = notificationManager.getNotificationChannel(ReminderConstants.channelId)
    if (existing != null) return

    notificationManager.createNotificationChannel(
        NotificationChannel(
            ReminderConstants.channelId,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.reminder_channel_description)
        },
    )
}
