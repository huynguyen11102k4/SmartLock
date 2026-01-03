package com.example.smartlock.repository

import com.example.smartlock.api.SmartLockApiService
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.model.entity.ICCard
import com.example.smartlock.model.iccard.AddICCardRequest
import com.example.smartlock.model.iccard.DeleteICCardRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ICCardRepository @Inject constructor(
    private val apiService: SmartLockApiService,
    private val database: AppDatabase
) {
    fun getICCardsFromDb(doorId: String): kotlinx.coroutines.flow.Flow<List<ICCard>> =
        database.icCardDao().getAllCards()

    suspend fun syncICCards(doorId: String): Result<List<ICCard>> {
        return try {
            val response = apiService.getICCards(doorId)
            if (response.isSuccessful) {
                val cards = response.body() ?: emptyList()
                database.icCardDao().deleteAll()
                database.icCardDao().insertAll(cards)
                Result.success(cards)
            } else {
                Result.failure(Exception("Lỗi đồng bộ thẻ: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addICCard(doorId: String, uid: String, name: String): Result<Boolean> {
        return try {
            val response = apiService.addICCard(doorId, AddICCardRequest(uid, name))
            if (response.isSuccessful) {
                val success = response.body() ?: false
                if (success) syncICCards(doorId)
                Result.success(success)
            } else {
                Result.failure(Exception("Không thể thêm thẻ"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteICCard(doorId: String, cardUid: String): Result<Unit> {
        return try {
            val response = apiService.deleteICCard(doorId, DeleteICCardRequest(cardUid))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                // Xóa khỏi DB local theo cardUid
                database.icCardDao().deleteCard(cardUid)
                Result.success(Unit)
            } else {
                val msg = response.body()?.errorMessage ?: "Delete IC card failed"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startSwipeAdd(doorId: String): Result<Boolean> {
        return try {
            val response = apiService.startSwipeAdd(doorId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: false)
            } else {
                Result.failure(Exception("Lỗi kích hoạt chế độ quét thẻ"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestSync(doorId: String): Result<Unit> {
        return try {
            val response = apiService.syncICCards(doorId)
            val apiResponse = response.body()

            if (response.isSuccessful && apiResponse?.isSuccess == true) {
                syncICCards(doorId)
                Result.success(Unit)
            } else {
                val msg = apiResponse?.errorMessage ?: "Yêu cầu đồng bộ thẻ từ thất bại"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}