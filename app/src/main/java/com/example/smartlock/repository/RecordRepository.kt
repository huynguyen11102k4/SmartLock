package com.example.smartlock.repository

import android.content.Context
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.Record
import kotlinx.coroutines.flow.Flow

class RecordRepository(context: Context) {
    private val recordDao = AppDatabase.getDatabase(context).recordDao()

    fun getRecordsForDoor(doorId: String): Flow<List<Record>> =
        recordDao.getRecordsForDoor(doorId)

    suspend fun insert(record: Record){
        recordDao.insert(record)
        recordDao.keepOnlyLatest100(record.doorId)
    }

    suspend fun deleteAllForDoor(doorId: String) = recordDao.deleteAllForDoor(doorId)
}