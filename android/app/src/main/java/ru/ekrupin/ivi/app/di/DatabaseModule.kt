package ru.ekrupin.ivi.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
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
import ru.ekrupin.ivi.data.local.dao.SyncPetMembershipDao
import ru.ekrupin.ivi.data.local.dao.SyncStateDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.SyncUserDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.db.MIGRATION_1_2
import ru.ekrupin.ivi.data.local.db.MIGRATION_2_3
import ru.ekrupin.ivi.data.local.db.MIGRATION_3_4
import ru.ekrupin.ivi.data.local.db.IviDatabase
import ru.ekrupin.ivi.data.sync.RoomSyncOutboxStore
import ru.ekrupin.ivi.data.sync.SyncOutboxStore
import ru.ekrupin.ivi.data.sync.SyncPayloadFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IviDatabase =
        Room.databaseBuilder(context, IviDatabase::class.java, "ivi.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun providePetDao(database: IviDatabase): PetDao = database.petDao()
    @Provides fun provideWeightEntryDao(database: IviDatabase): WeightEntryDao = database.weightEntryDao()
    @Provides fun provideEventTypeDao(database: IviDatabase): EventTypeDao = database.eventTypeDao()
    @Provides fun providePetEventDao(database: IviDatabase): PetEventDao = database.petEventDao()
    @Provides fun provideReminderSettingsDao(database: IviDatabase): ReminderSettingsDao = database.reminderSettingsDao()
    @Provides fun provideSyncOutboxDao(database: IviDatabase): SyncOutboxDao = database.syncOutboxDao()
    @Provides fun provideSyncUserDao(database: IviDatabase): SyncUserDao = database.syncUserDao()
    @Provides fun provideSyncPetMembershipDao(database: IviDatabase): SyncPetMembershipDao = database.syncPetMembershipDao()
    @Provides fun provideSyncStateDao(database: IviDatabase): SyncStateDao = database.syncStateDao()
    @Provides fun provideSyncPayloadFactory(): SyncPayloadFactory = SyncPayloadFactory()
    @Provides fun provideSyncOutboxStore(syncOutboxDao: SyncOutboxDao): SyncOutboxStore = RoomSyncOutboxStore(syncOutboxDao)
}
