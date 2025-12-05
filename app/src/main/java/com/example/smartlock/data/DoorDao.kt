package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.Door
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorDao {
    @Query("SELECT * FROM doors ORDER BY name ASC")
    fun getAllDoors(): Flow<List<Door>>

    @Query("SELECT * FROM doors WHERE id = :doorId")
    suspend fun getDoorById(doorId: String): Door?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoor(door: Door)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoors(doors: List<Door>)

    @Update
    suspend fun updateDoor(door: Door)

    @Delete
    suspend fun deleteDoor(door: Door)

    @Query("DELETE FROM doors WHERE id = :doorId")
    suspend fun deleteDoorById(doorId: String)

    @Query("DELETE FROM doors")
    suspend fun deleteAll()

    @Query("UPDATE doors SET masterPasscode = :code WHERE id = :doorId")
    suspend fun updateMasterPasscode(doorId: String, code: String)
}