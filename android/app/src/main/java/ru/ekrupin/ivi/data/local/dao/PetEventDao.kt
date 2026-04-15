package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.model.ReminderScheduleEntry

@Dao
interface PetEventDao {
    @Query("SELECT COUNT(*) FROM pet_events")
    suspend fun count(): Int

    @Query("SELECT * FROM pet_events WHERE deletedAt IS NULL ORDER BY eventDate DESC")
    fun observeAll(): Flow<List<PetEventEntity>>

    @Query("SELECT * FROM pet_events WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<PetEventEntity?>

    @Query("SELECT * FROM pet_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PetEventEntity?

    @Query("SELECT * FROM pet_events WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): PetEventEntity?

    @Query(
        """
        SELECT
            pet_events.id AS eventId,
            event_types.name AS eventTypeName,
            pet_events.dueDate AS dueDate,
            pet_events.notificationsEnabled AS notificationsEnabled,
            pet_events.status AS status
        FROM pet_events
        INNER JOIN event_types ON event_types.id = pet_events.eventTypeId
        WHERE pet_events.deletedAt IS NULL AND event_types.deletedAt IS NULL
        """,
    )
    suspend fun getReminderEntries(): List<ReminderScheduleEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PetEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<PetEventEntity>)

    @Query("UPDATE pet_events SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ru.ekrupin.ivi.domain.model.PetEventStatus, updatedAt: java.time.LocalDateTime)

    @Query("SELECT COUNT(*) FROM pet_events WHERE eventTypeId = :eventTypeId AND deletedAt IS NULL")
    suspend fun countByEventTypeId(eventTypeId: Long): Int

    @Query("DELETE FROM pet_events")
    suspend fun deleteAll()
}
