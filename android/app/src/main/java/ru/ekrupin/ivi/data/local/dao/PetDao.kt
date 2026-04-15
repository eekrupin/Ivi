package ru.ekrupin.ivi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.local.entity.PetEntity

@Dao
interface PetDao {
    @Query("SELECT COUNT(*) FROM pets")
    suspend fun count(): Int

    @Query("SELECT * FROM pets WHERE deletedAt IS NULL LIMIT 1")
    fun observePet(): Flow<PetEntity?>

    @Query("SELECT * FROM pets LIMIT 1")
    suspend fun getPet(): PetEntity?

    @Query("DELETE FROM pets")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: PetEntity)

    @Update
    suspend fun update(pet: PetEntity)
}
