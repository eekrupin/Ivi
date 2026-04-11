package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.ReminderSettingsEntity

@Dao
interface ReminderSettingsDao {
    @Query("SELECT * FROM reminder_settings LIMIT 1")
    fun observe(): Flow<ReminderSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: ReminderSettingsEntity)
}
