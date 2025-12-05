package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartlock.model.ICCard
import kotlinx.coroutines.flow.Flow

@Dao
interface ICCardDao {
    @Query("SELECT * FROM ic_cards WHERE doorId = :doorId")
    fun getCardsForDoor(doorId: String): Flow<List<ICCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: ICCard)

    @Query("DELETE FROM ic_cards WHERE id = :id AND doorId = :doorId")
    suspend fun delete(id: String, doorId: String)

    @Query("DELETE FROM ic_cards WHERE doorId = :doorId")
    suspend fun deleteAllForDoor(doorId: String)
}