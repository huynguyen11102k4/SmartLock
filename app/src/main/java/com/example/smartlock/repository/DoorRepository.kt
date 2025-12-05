package com.example.smartlock.repository

import android.content.Context
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.Door
import kotlinx.coroutines.flow.Flow

class DoorRepository(context: Context) {
    private val doorDao = AppDatabase.Companion.getDatabase(context).doorDao()

    val allDoors: Flow<List<Door>> = doorDao.getAllDoors()

    suspend fun insert(door: Door) = doorDao.insertDoor(door)
    suspend fun update(door: Door) = doorDao.updateDoor(door)
    suspend fun delete(door: Door) = doorDao.deleteDoor(door)
    suspend fun deleteById(doorId: String) = doorDao.deleteDoorById(doorId)
    suspend fun getDoorById(doorId: String) = doorDao.getDoorById(doorId)
    suspend fun updateMasterPasscode(doorId: String, newCode: String) {
        doorDao.updateMasterPasscode(doorId, newCode)
    }
}