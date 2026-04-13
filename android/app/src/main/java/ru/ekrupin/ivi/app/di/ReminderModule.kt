package ru.ekrupin.ivi.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.ekrupin.ivi.data.reminder.AndroidReminderScheduler
import ru.ekrupin.ivi.data.reminder.ReminderScheduler

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AndroidReminderScheduler): ReminderScheduler
}
