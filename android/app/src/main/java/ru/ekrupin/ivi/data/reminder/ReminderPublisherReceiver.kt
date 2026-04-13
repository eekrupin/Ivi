package ru.ekrupin.ivi.data.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.ekrupin.ivi.R

@AndroidEntryPoint
class ReminderPublisherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.ensureReminderNotificationChannel()

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = intent.getIntExtra(ReminderConstants.extraNotificationId, 0)
        val title = intent.getStringExtra(ReminderConstants.extraNotificationTitle).orEmpty()
        val text = intent.getStringExtra(ReminderConstants.extraNotificationText).orEmpty()

        NotificationManagerCompat.from(context).notify(
            notificationId,
            NotificationCompat.Builder(context, ReminderConstants.channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build(),
        )
    }
}
