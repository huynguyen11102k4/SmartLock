package com.example.smartlock.repository

import com.example.smartlock.api.SmartLockApiService
import com.example.smartlock.model.auth.AuthResponse
import com.example.smartlock.model.auth.ChangePasswordRequest
import com.example.smartlock.model.auth.ForgotPasswordRequest
import com.example.smartlock.model.auth.LinkOAuthRequest
import com.example.smartlock.model.auth.LoginRequest
import com.example.smartlock.model.auth.LogoutRequest
import com.example.smartlock.model.auth.RegisterRequest
import com.example.smartlock.model.auth.UnlinkOAuthRequest
import com.example.smartlock.model.auth.VerifyOtpRequest
import com.example.smartlock.utils.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: SmartLockApiService,
    private val tokenManager: TokenManager
) {
    suspend fun register(email: String, password: String, name: String): Result<Unit> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, name))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Registration failed"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                android.util.Log.e("API_ERROR", "Register Failed 500: $errorJson")
                Result.failure(Exception("Registration failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyRegisterOtp(email: String, otp: String): Result<Unit> {
        return try {
            val response = apiService.verifyRegisterOtp(VerifyOtpRequest(email, otp))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "OTP verification failed"))
                }
            } else {
                Result.failure(Exception("OTP Verification failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendRegisterOtp(email: String): Result<Unit> {
        return try {
            val response = apiService.resendRegisterOtp(ForgotPasswordRequest(email))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Resend OTP failed"))
                }
            } else {
                Result.failure(Exception("Resend OTP failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String, type: String = "Local"): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(type, email, password, null))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.value != null) {
                    val authResponse = apiResponse.value
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken, email)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Login failed"))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenManager.getRefreshToken() ?: ""
            val response = apiService.logout(LogoutRequest(refreshToken))
            tokenManager.clearTokens()

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Logout failed"))
                }
            } else {
                Result.failure(Exception("Logout failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val response = apiService.changePassword(ChangePasswordRequest(oldPassword, newPassword))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Change password failed"))
                }
            } else {
                Result.failure(Exception("Change password failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendForgotPasswordOtp(email: String): Result<Unit> {
        return try {
            val response = apiService.sendForgotPasswordOtp(ForgotPasswordRequest(email))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Send OTP failed"))
                }
            } else {
                Result.failure(Exception("Send OTP failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyForgotPasswordOtp(email: String, otp: String, newPassword: String): Result<Unit> {
        return try {
            val response = apiService.verifyForgotPasswordOtp(VerifyOtpRequest(email, otp, newPassword))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Verify OTP failed"))
                }
            } else {
                Result.failure(Exception("Verify OTP failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithSocial(type: String, token: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(type, null, null, token))
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.value != null) {
                    val authResponse = apiResponse.value
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception(apiResponse.errorMessage ?: "Login failed"))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkOAuth(provider: String, token: String): Result<Unit> {
        return try {
            val response = apiService.linkOAuth(LinkOAuthRequest(provider, token))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Link OAuth failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkOAuth(provider: String): Result<Unit> {
        return try {
            val response = apiService.unlinkOAuth(UnlinkOAuthRequest(provider))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.errorMessage ?: "Unlink OAuth failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}