package com.example.smartlock.api

import android.util.Log
import com.example.smartlock.model.auth.RefreshTokenRequest
import com.example.smartlock.utils.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val refreshApi: RefreshApi
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= 2) {
            Log.e("AUTH_DEBUG", "Đã thử refresh quá 2 lần, dừng lại.")
            return null
        }

        val currentRefreshToken = tokenManager.getRefreshToken()
        val userEmail = tokenManager.getUserEmail()

        if (currentRefreshToken.isNullOrEmpty() || userEmail.isNullOrEmpty()) {
            Log.e("AUTH_DEBUG", "Thiếu Refresh Token hoặc Email, yêu cầu đăng nhập lại.")
            return null
        }

        return runBlocking {
            mutex.withLock {
                val latestRefreshToken = tokenManager.getRefreshToken()

                if (latestRefreshToken != currentRefreshToken) {
                    Log.d("AUTH_DEBUG", "Token đã được refresh bởi luồng khác.")
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokenManager.getAccessToken()}")
                        .build()
                }

                try {
                    Log.d("AUTH_DEBUG", "Đang tiến hành gọi API Refresh Token...")
                    val apiResponse = refreshApi.refreshToken(
                        RefreshTokenRequest(latestRefreshToken!!, userEmail)
                    )

                    if (apiResponse.isSuccess && apiResponse.value != null) {
                        val authResponse = apiResponse.value!!
                        Log.d("AUTH_DEBUG", "Refresh thành công! Lưu token mới.")

                        tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${authResponse.accessToken}")
                            .build()
                    } else {
                        Log.e("AUTH_DEBUG", "Server từ chối Refresh: ${apiResponse.errorMessage}")
                        tokenManager.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    Log.e("AUTH_DEBUG", "Lỗi hệ thống khi Refresh: ${e.message}")
                    null
                }
            }
        }
    }
}

private val Response.responseCount: Int
    get() {
        var result = 1
        var priorResponse = priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }