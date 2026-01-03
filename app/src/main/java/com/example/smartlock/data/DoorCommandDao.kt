package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.entity.DoorCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorCommandDao {
    @Query("SELECT * FROM door_commands WHERE doorId = :doorId ORDER BY sentAt DESC")
    fun getCommandsByDoor(doorId: String): Flow<List<DoorCommand>>

    @Query("SELECT * FROM door_commands WHERE id = :commandId")
    fun getCommandById(commandId: String): Flow<DoorCommand?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: DoorCommand)

    @Update
    suspend fun update(command: DoorCommand)

    @Query("DELETE FROM door_commands WHERE doorId = :doorId")
    suspend fun deleteByDoor(doorId: String)
}