package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.SyncConflictEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts ORDER BY conflictedAt DESC")
    fun observeAll(): Flow<List<SyncConflictEntity>>

    @Query("SELECT * FROM sync_conflicts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SyncConflictEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncConflictEntity): Long

    @Query("DELETE FROM sync_conflicts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_conflicts WHERE entityType = :entityType AND entityLocalId = :entityLocalId")
    suspend fun deleteByEntity(entityType: SyncEntityType, entityLocalId: Long)

    @Query("DELETE FROM sync_conflicts WHERE clientMutationId = :clientMutationId")
    suspend fun deleteByClientMutationId(clientMutationId: String)

    @Query("DELETE FROM sync_conflicts")
    suspend fun deleteAll()
}
