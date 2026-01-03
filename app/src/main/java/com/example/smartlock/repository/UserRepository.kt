package com.example.smartlock.repository

import android.util.Log
import com.example.smartlock.api.SmartLockApiService
import com.example.smartlock.model.entity.User
import com.example.smartlock.model.userprofile.UpdateAvatarRequest
import com.example.smartlock.model.userprofile.UpdateDateOfBirthRequest
import com.example.smartlock.model.userprofile.UpdateNameRequest
import com.example.smartlock.model.userprofile.UpdatePhoneRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: SmartLockApiService
) {
    suspend fun getUserProfile(): Result<User> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(apiResponse.value!!)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Unknown error"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                Log.e("UserRepository", "Get Profile Failed: ${response.code()} - $errorJson")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Get Profile Exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateName(name: String): Result<Unit> {
        return try {
            val response = apiService.updateName(UpdateNameRequest(name))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val msg = response.body()?.errorMessage ?: "Failed to update name"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAvatar(filePart: MultipartBody.Part?, isRandom: Boolean): Result<Unit> {
        return try {
            val finalPart = if (isRandom) {
                val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", "", emptyBody)
            } else {
                filePart
            }

            val response = apiService.updateAvatar(isRandom, finalPart)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val errorJson = response.errorBody()?.string()
                Result.failure(Exception(errorJson ?: "Lỗi cập nhật"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        return try {
            val response = apiService.updatePhoneNumber(UpdatePhoneRequest(phoneNumber))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val msg = response.body()?.errorMessage ?: "Failed to update phone number"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDateOfBirth(dateOfBirth: String): Result<Unit> {
        return try {
            val response = apiService.updateDateOfBirth(UpdateDateOfBirthRequest(dateOfBirth))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val msg = response.body()?.errorMessage ?: "Failed to update date of birth"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}