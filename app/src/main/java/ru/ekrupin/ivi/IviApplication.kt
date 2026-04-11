package ru.ekrupin.ivi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.seed.DatabaseSeeder
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.data.reminder.ensureReminderNotificationChannel

@HiltAndroidApp
class IviApplication : Application() {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        ensureReminderNotificationChannel()
        databaseSeeder.seedIfNeeded()
        reminderScheduler.refreshAll()
    }
}
