package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.ekrupin.ivi.data.local.entity.SyncPetMembershipEntity

@Dao
interface SyncPetMembershipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncPetMembershipEntity>)

    @Query("DELETE FROM sync_pet_memberships")
    suspend fun deleteAll()
}
