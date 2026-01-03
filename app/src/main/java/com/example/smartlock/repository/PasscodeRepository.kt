package com.example.smartlock.repository

import com.example.smartlock.api.SmartLockApiService
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.entity.Passcode
import com.example.smartlock.model.passcode.AddPasscodeRequest
import com.example.smartlock.model.passcode.DeletePasscodeRequest
import com.example.smartlock.model.passcode.UpdatePasscodeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasscodeRepository @Inject constructor(
    private val apiService: SmartLockApiService,
    private val database: AppDatabase
) {
    fun getPasscodesFromDb(doorId: String): kotlinx.coroutines.flow.Flow<List<Passcode>> =
        database.passcodeDao().getAllPasscodes()

    suspend fun syncPasscodes(doorId: String): Result<List<Passcode>> {
        return try {
            val response = apiService.getPasscodes(doorId)
            if (response.isSuccessful) {
                val passcodes = response.body() ?: emptyList()
                database.passcodeDao().deleteAll()
                database.passcodeDao().insertAll(passcodes)
                Result.success(passcodes)
            } else {
                Result.failure(Exception("Lỗi: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addPasscode(doorId: String, code: String, type: Int, vFrom: String?, vTo: String?): Result<Unit> {
        return try {
            val response = apiService.addPasscode(doorId, AddPasscodeRequest(code, type, vFrom, vTo))
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                syncPasscodes(doorId)
                Result.success(Unit)
            } else {
                val errorMsg = apiResponse?.errorMessage ?: "Lỗi không xác định khi thêm passcode"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePasscode(
        doorId: String,
        code: String,
        type: Int,
        validFrom: String? = null,
        validTo: String? = null
    ): Result<Unit> {
        return try {
            val response = apiService.updatePasscode(
                doorId,
                UpdatePasscodeRequest(code, type, validFrom, validTo)
            )
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                syncPasscodes(doorId)
                Result.success(Unit)
            } else {
                val msg = apiResponse?.errorMessage ?: "Update passcode failed"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePasscode(doorId: String, code: String): Result<Unit> {
        return try {
            val response = apiService.deletePasscode(doorId, DeletePasscodeRequest(code))
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                database.passcodeDao().deletePasscode(code)
                Result.success(Unit)
            } else {
                val msg = apiResponse?.errorMessage ?: "Delete passcode failed"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestSync(doorId: String): Result<Unit> {
        return try {
            val response = apiService.syncPasscodes(doorId)
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                syncPasscodes(doorId)
                Result.success(Unit)
            } else {
                val msg = apiResponse?.errorMessage ?: "Request sync passcodes failed"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}