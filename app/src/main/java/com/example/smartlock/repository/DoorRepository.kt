package com.example.smartlock.repository

import android.util.Log
import com.example.smartlock.api.SmartLockApiService
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.door.CreateDoorRequest
import com.example.smartlock.model.door.UpdateDoorCodeRequest
import com.example.smartlock.model.door.UpdateDoorRequest
import com.example.smartlock.model.doorshare.ShareDoorRequest
import com.example.smartlock.model.doorshare.UpdateShareRequest
import com.example.smartlock.model.entity.Door
import com.example.smartlock.model.entity.DoorRecord
import com.example.smartlock.model.entity.DoorShare
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoorRepository @Inject constructor(
    private val apiService: SmartLockApiService,
    private val database: AppDatabase,
) {
    fun getDoorsFromDb(): Flow<List<Door>> = database.doorDao().getAllDoors()

    fun getDoorById(doorId: String): Flow<Door?> = database.doorDao().getDoorById(doorId)

    suspend fun syncDoors(): Result<List<Door>> {
        return try {
            val response = apiService.getDoors()
            if (response.isSuccessful) {
                val rawJson = response.body().toString()
                android.util.Log.d("DEBUG_NULL", "Dữ liệu thô từ Server: $rawJson") // Log này sẽ cho bạn thấy danh sách các Door

                val doors = response.body() ?: emptyList()

                doors.forEach { door ->
                    android.util.Log.d("DEBUG_NULL", "Kiểm tra Door ID: ${door.id}")
                    if (door.ownerId == null) android.util.Log.e("DEBUG_NULL", "TRƯỜNG ownerId BỊ NULL!")
                    if (door.mqttTopicPrefix == null) android.util.Log.e("DEBUG_NULL", "TRƯỜNG mqttTopicPrefix BỊ NULL!")
                }

                database.doorDao().insertAll(doors)
                Result.success(doors)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DEBUG_NULL", "Lỗi Parse hoặc Null: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createDoor(
        doorCode: String,
        name: String,
        mqttTopicPrefix: String,
        macAddress: String
    ): Result<Door> {
        return try {
            val response = apiService.createDoor(
                CreateDoorRequest(doorCode, name, mqttTopicPrefix, macAddress)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val doorId = response.body()!!.value!!
                getDoor(doorId)
            } else {
                val jsonMsg = response.body()?.errorMessage ?: "Failed: ${response.code()}"
                Log.e("DoorRepository", "Create Door Failed: $jsonMsg")
                Result.failure(Exception(response.body()?.errorMessage ?: "Create door failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoor(doorId: String): Result<Door> {
        return try {
            val response = apiService.getDoor(doorId)
            if (response.isSuccessful && response.body() != null) {
                val door = response.body()!!
                database.doorDao().insert(door)
                Result.success(door)
            } else {
                Result.failure(Exception("Không tìm thấy thiết bị"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDoor(doorId: String, name: String): Result<Unit> {
        return try {
            val response = apiService.updateDoor(doorId, UpdateDoorRequest(name))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Update failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDoor(doorId: String): Result<Unit> {
        return try {
            val response = apiService.deleteDoor(doorId)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                database.doorDao().deleteDoor(doorId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDoorCode(doorId: String, newCode: String): Result<Unit> {
        return try {
            val response = apiService.updateDoorCode(doorId, UpdateDoorCodeRequest(newCode))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
//                syncDoors()
                database.doorDao().updateDoorCode(doorId, newCode)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Đổi mã khóa chính thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lockDoor(doorId: String): Result<Unit> {
        return try {
            val response = apiService.lockDoor(doorId)
            val apiResponse = response.body()
            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(apiResponse?.errorMessage ?: "Khóa cửa thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockDoor(doorId: String): Result<Unit> {
        return try {
            val response = apiService.unlockDoor(doorId)
            val apiResponse = response.body()
            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(apiResponse?.errorMessage ?: "Mở khóa thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncDoorStatus(doorId: String): Result<Unit> {
        return try {
            val response = apiService.syncDoorStatus(doorId)
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                val doorResponse = apiService.getDoor(doorId)
                if (doorResponse.isSuccessful && doorResponse.body() != null) {
                    database.doorDao().insert(doorResponse.body()!!)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Không thể fetch thông tin trạng thái mới"))
                }
            } else {
                Result.failure(Exception(apiResponse?.errorMessage ?: "Đồng bộ thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRecordsFromDb(): Flow<List<DoorRecord>> =
        database.doorRecordDao().getAllRecords()

    suspend fun syncRecords(doorId: String): Result<List<DoorRecord>> {
        return try {
            val response = apiService.getDoorRecords(doorId)
            if (response.isSuccessful) {
                val records = response.body() ?: emptyList()

                database.doorRecordDao().deleteAll()
                database.doorRecordDao().insertAll(records)

                Result.success(records)
            } else {
                Result.failure(Exception("Lỗi đồng bộ lịch sử"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoorRecord(doorId: String, recordId: String): Result<DoorRecord> {
        return try {
            val response = apiService.getDoorRecord(doorId, recordId)
            if (response.isSuccessful && response.body() != null) {
                val record = response.body()!!
                database.doorRecordDao().insert(record)
                Result.success(record)
            } else {
                Result.failure(Exception("Không tìm thấy bản ghi"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSharesFromDb(doorId: String): Flow<List<DoorShare>> =
        database.doorShareDao().getSharesByDoor(doorId)

    suspend fun syncShares(doorId: String): Result<List<DoorShare>> {
        return try {
            val response = apiService.getDoorShares(doorId)
            if (response.isSuccessful && response.body() != null) {
                val shares = response.body()!!
                database.doorShareDao().insertAll(shares)
                Result.success(shares)
            } else {
                Result.failure(Exception("Sync shares failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareDoor(
        doorId: String,
        userId: String,
        permission: Int,
        validFrom: String? = null,
        validTo: String? = null
    ): Result<Unit> {
        return try {
            val response = apiService.shareDoor(
                doorId,
                ShareDoorRequest(userId, permission, validFrom, validTo)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                syncShares(doorId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Share failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateShare(
        doorId: String,
        userId: String,
        permission: Int,
        validFrom: String? = null,
        validTo: String? = null
    ): Result<Unit> {
        return try {
            val response = apiService.updateShare(
                doorId, userId, UpdateShareRequest(permission, validFrom, validTo)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                syncShares(doorId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Update share failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeShare(doorId: String, userId: String): Result<Unit> {
        return try {
            val response = apiService.revokeShare(doorId, userId)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                database.doorShareDao().deleteShare(doorId, userId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Revoke failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}