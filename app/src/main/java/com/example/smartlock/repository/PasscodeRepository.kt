package com.example.smartlock.repository

import android.content.Context
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.Passcode
import kotlinx.coroutines.flow.Flow

class PasscodeRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).passcodeDao()

    fun getPasscodesForDoor(doorId: String) : Flow<List<Passcode>>
        = dao.getPasscodesForDoor(doorId)

    suspend fun insert(passcode: Passcode) = dao.insert(passcode)

    suspend fun delete(code: String, doorId: String) = dao.delete(code, doorId)

    suspend fun deleteAllForDoor(doorId: String) = dao.deleteAllForDoor(doorId)
}