package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartlock.model.Passcode
import kotlinx.coroutines.flow.Flow

@Dao
interface PasscodeDao {
    @Query("SELECT * FROM passcodes WHERE doorId = :doorId ORDER BY code ASC")
    fun getPasscodesForDoor(doorId: String): Flow<List<Passcode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(passcode: Passcode)

    @Query("DELETE FROM passcodes WHERE code = :code AND doorId = :doorId")
    suspend fun delete(code: String, doorId: String)

    @Query("DELETE FROM passcodes WHERE doorId = :doorId")
    suspend fun deleteAllForDoor(doorId: String)

}