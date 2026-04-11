package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity

@Dao
interface EventTypeDao {
    @Query("SELECT COUNT(*) FROM event_types")
    suspend fun count(): Int

    @Query("SELECT * FROM event_types ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EventTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventType: EventTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventTypes: List<EventTypeEntity>)
}
