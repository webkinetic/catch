package com.catch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Insert
    suspend fun insert(capture: CaptureEntity): Long

    @Update
    suspend fun update(capture: CaptureEntity)

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: Long): CaptureEntity?

    @Query("SELECT * FROM captures ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE state = :state ORDER BY capturedAt ASC")
    fun observeByState(state: CaptureState): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE state = :state ORDER BY capturedAt ASC")
    suspend fun getByState(state: CaptureState): List<CaptureEntity>

    /** Feeds StructureRequest.recentTags/recentPeople — "the Henderson thing" only resolves with this. */
    @Query("SELECT * FROM captures WHERE id != :excludingId ORDER BY capturedAt DESC LIMIT :limit")
    suspend fun getRecent(excludingId: Long, limit: Int = 30): List<CaptureEntity>
}
