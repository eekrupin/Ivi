package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.PetEventEntity

@Dao
interface PetEventDao {
    @Query("SELECT COUNT(*) FROM pet_events")
    suspend fun count(): Int

    @Query("SELECT * FROM pet_events ORDER BY eventDate DESC")
    fun observeAll(): Flow<List<PetEventEntity>>

    @Query("SELECT * FROM pet_events WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<PetEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PetEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<PetEventEntity>)

    @Query("UPDATE pet_events SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ru.ekrupin.ivi.domain.model.PetEventStatus, updatedAt: java.time.LocalDateTime)
}
