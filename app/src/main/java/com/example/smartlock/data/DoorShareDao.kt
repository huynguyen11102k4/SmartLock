package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.entity.DoorShare
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorShareDao {
    @Query("SELECT * FROM door_shares WHERE doorId = :doorId")
    fun getSharesByDoor(doorId: String): Flow<List<DoorShare>>

    @Query("SELECT * FROM door_shares WHERE userId = :userId")
    fun getSharesByUser(userId: String): Flow<List<DoorShare>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(share: DoorShare)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shares: List<DoorShare>)

    @Update
    suspend fun update(share: DoorShare)

    @Query("DELETE FROM door_shares WHERE doorId = :doorId AND userId = :userId")
    suspend fun deleteShare(doorId: String, userId: String)

    @Query("DELETE FROM door_shares WHERE doorId = :doorId")
    suspend fun deleteByDoor(doorId: String)
}