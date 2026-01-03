package com.example.smartlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartlock.model.entity.ICCard
import kotlinx.coroutines.flow.Flow

@Dao
interface ICCardDao {
    @Query("SELECT * FROM ic_cards WHERE isActive = 1 ORDER BY name ASC")
    fun getAllCards(): Flow<List<ICCard>>

    @Query("SELECT * FROM ic_cards WHERE id = :cardId")
    fun getCardById(cardId: String): Flow<ICCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: ICCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<ICCard>)

    @Update
    suspend fun update(card: ICCard)

    @Query("DELETE FROM ic_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM ic_cards")
    suspend fun deleteAll()
}