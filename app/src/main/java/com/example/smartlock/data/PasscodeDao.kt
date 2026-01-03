package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.entity.Passcode
import kotlinx.coroutines.flow.Flow

@Dao
interface PasscodeDao {
    @Query("SELECT * FROM passcodes WHERE isActive = 1 ORDER BY type DESC")
    fun getAllPasscodes(): Flow<List<Passcode>>

    @Query("SELECT * FROM passcodes WHERE id = :passcodeId")
    fun getPasscodeById(passcodeId: String): Flow<Passcode?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(passcode: Passcode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(passcodes: List<Passcode>)

    @Update
    suspend fun update(passcode: Passcode)

    @Query("DELETE FROM passcodes WHERE id = :passcodeId")
    suspend fun deletePasscode(passcodeId: String)

    @Query("DELETE FROM passcodes")
    suspend fun deleteAll()
}