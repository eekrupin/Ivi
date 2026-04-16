package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncOutboxEntity): Long

    @Query("SELECT * FROM sync_outbox WHERE status = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getByStatus(status: SyncOutboxStatus, limit: Int): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = :status")
    suspend fun countByStatus(status: SyncOutboxStatus): Int

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun countAll(): Int

    @Query("UPDATE sync_outbox SET status = :status, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateStatus(ids: List<Long>, status: SyncOutboxStatus, updatedAt: java.time.LocalDateTime)

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entityLocalId = :entityLocalId")
    suspend fun deleteByEntity(entityType: SyncEntityType, entityLocalId: Long)
}
