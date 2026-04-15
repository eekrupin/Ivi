package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.ekrupin.ivi.data.local.entity.SyncUserEntity

@Dao
interface SyncUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncUserEntity>)

    @Query("DELETE FROM sync_users")
    suspend fun deleteAll()
}
