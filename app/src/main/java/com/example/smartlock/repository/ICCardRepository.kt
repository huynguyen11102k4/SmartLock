package com.example.smartlock.repository

import android.content.Context
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.ICCard
import kotlinx.coroutines.flow.Flow

class ICCardRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).icCardDao()

    fun getCardsForDoor(doorId: String): Flow<List<ICCard>> =
        dao.getCardsForDoor(doorId)

    suspend fun insert(card: ICCard) = dao.insert(card)
    suspend fun delete(id: String, doorId: String) = dao.delete(id, doorId)
    suspend fun deleteAllForDoor(doorId: String) = dao.deleteAllForDoor(doorId)
}