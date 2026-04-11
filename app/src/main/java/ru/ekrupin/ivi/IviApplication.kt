package ru.ekrupin.ivi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import ru.ekrupin.ivi.data.local.seed.DatabaseSeeder

@HiltAndroidApp
class IviApplication : Application() {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    override fun onCreate() {
        super.onCreate()
        databaseSeeder.seedIfNeeded()
    }
}
