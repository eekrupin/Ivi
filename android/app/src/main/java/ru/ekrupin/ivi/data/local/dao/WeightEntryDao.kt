package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity

@Dao
interface WeightEntryDao {
    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun count(): Int

    @Query("SELECT * FROM weight_entries WHERE deletedAt IS NULL ORDER BY date DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weightEntry: WeightEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(weightEntries: List<WeightEntryEntity>)

    @Query("SELECT * FROM weight_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): WeightEntryEntity?

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()
}
