package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartlock.model.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE doorId = :doorId ORDER BY timestamp DESC")
    fun getRecordsForDoor(doorId: String): Flow<List<Record>>

    @Query("DELETE FROM records WHERE doorId = :doorId")
    suspend fun deleteAllForDoor(doorId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record)

    @Query("""
        DELETE FROM records
        WHERE doorId = :doorId
        AND id NOT IN (
            SELECT id FROM records
            WHERE doorId = :doorId
            ORDER BY timestamp DESC
            LIMIT 100
        )
    """)
    suspend fun keepOnlyLatest100(doorId: String)
}