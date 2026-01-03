package com.example.smartlock.data

import androidx.room.*
import com.example.smartlock.model.entity.DoorRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorRecordDao {
    @Query("SELECT * FROM door_records ORDER BY occurredAt DESC LIMIT :limit")
    fun getAllRecords(limit: Int = 100): Flow<List<DoorRecord>>

    @Query("SELECT * FROM door_records WHERE id = :recordId")
    fun getRecordById(recordId: String): Flow<DoorRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DoorRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DoorRecord>)

    @Query("DELETE FROM door_records")
    suspend fun deleteAll()
}