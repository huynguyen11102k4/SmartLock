package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.entity.Door
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorDao {
    @Query("SELECT * FROM doors ORDER BY name ASC")
    fun getAllDoors(): Flow<List<Door>>

    @Query("SELECT * FROM doors WHERE id = :doorId")
    fun getDoorById(doorId: String): Flow<Door?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(door: Door)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doors: List<Door>)

    @Update
    suspend fun update(door: Door)

    @Query("UPDATE doors SET doorCode = :newCode WHERE id = :doorId")
    suspend fun updateDoorCode(doorId: String, newCode: String?)

    @Query("DELETE FROM doors WHERE id = :doorId")
    suspend fun deleteDoor(doorId: String)

    @Query("DELETE FROM doors")
    suspend fun deleteAll()


}