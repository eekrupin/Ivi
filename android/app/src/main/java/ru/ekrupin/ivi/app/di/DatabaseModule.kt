package ru.ekrupin.ivi.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.ReminderSettingsDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.IviDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IviDatabase =
        Room.databaseBuilder(context, IviDatabase::class.java, "ivi.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePetDao(database: IviDatabase): PetDao = database.petDao()
    @Provides fun provideWeightEntryDao(database: IviDatabase): WeightEntryDao = database.weightEntryDao()
    @Provides fun provideEventTypeDao(database: IviDatabase): EventTypeDao = database.eventTypeDao()
    @Provides fun providePetEventDao(database: IviDatabase): PetEventDao = database.petEventDao()
    @Provides fun provideReminderSettingsDao(database: IviDatabase): ReminderSettingsDao = database.reminderSettingsDao()
}
