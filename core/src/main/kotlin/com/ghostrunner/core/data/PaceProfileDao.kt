package com.ghostrunner.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaceProfileDao {

    @Query("SELECT * FROM pace_profile ORDER BY isDefault DESC, id ASC")
    fun observeAll(): Flow<List<PaceProfileEntity>>

    @Query("SELECT * FROM pace_profile WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PaceProfileEntity?

    @Query("SELECT * FROM pace_profile WHERE isDefault = 1 LIMIT 1")
    suspend fun findDefault(): PaceProfileEntity?

    @Insert
    suspend fun insert(profile: PaceProfileEntity): Long

    @Update
    suspend fun update(profile: PaceProfileEntity)

    @Delete
    suspend fun delete(profile: PaceProfileEntity)

    @Query("UPDATE pace_profile SET isDefault = 0")
    suspend fun clearDefaults()

    @Transaction
    suspend fun setDefault(id: Long) {
        clearDefaults()
        val existing = findById(id) ?: return
        update(existing.copy(isDefault = true))
    }
}
