package ru.ekrupin.ivi

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.seed.DatabaseSeeder
import ru.ekrupin.ivi.data.reminder.ReminderScheduler
import ru.ekrupin.ivi.data.reminder.ensureReminderNotificationChannel
import ru.ekrupin.ivi.data.sync.background.BackgroundSyncScheduler

@HiltAndroidApp
class IviApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backgroundSyncScheduler: BackgroundSyncScheduler

    override fun onCreate() {
        super.onCreate()
        ensureReminderNotificationChannel()

        // Demo seed data is useful during local development, but release builds
        // must start with an empty database for real usage.
        val isDebuggableBuild = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggableBuild) {
            databaseSeeder.seedIfNeeded()
        }

        reminderScheduler.refreshAll()
        backgroundSyncScheduler.ensureScheduled()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
