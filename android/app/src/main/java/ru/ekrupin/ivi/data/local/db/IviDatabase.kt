package ru.ekrupin.ivi.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.ekrupin.ivi.data.local.converter.LocalDateConverters
import ru.ekrupin.ivi.data.local.dao.EventTypeDao
import ru.ekrupin.ivi.data.local.dao.PetDao
import ru.ekrupin.ivi.data.local.dao.PetEventDao
import ru.ekrupin.ivi.data.local.dao.ReminderSettingsDao
import ru.ekrupin.ivi.data.local.dao.SyncPetMembershipDao
import ru.ekrupin.ivi.data.local.dao.SyncStateDao
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.dao.SyncUserDao
import ru.ekrupin.ivi.data.local.dao.WeightEntryDao
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.ReminderSettingsEntity
import ru.ekrupin.ivi.data.local.entity.SyncPetMembershipEntity
import ru.ekrupin.ivi.data.local.entity.SyncStateEntity
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.local.entity.SyncUserEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        PetEntity::class,
        WeightEntryEntity::class,
        EventTypeEntity::class,
        PetEventEntity::class,
        ReminderSettingsEntity::class,
        SyncOutboxEntity::class,
        SyncUserEntity::class,
        SyncPetMembershipEntity::class,
        SyncStateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(LocalDateConverters::class)
abstract class IviDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun petEventDao(): PetEventDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncUserDao(): SyncUserDao
    abstract fun syncPetMembershipDao(): SyncPetMembershipDao
    abstract fun syncStateDao(): SyncStateDao
}
