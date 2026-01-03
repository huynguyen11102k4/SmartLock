package com.example.smartlock.api

import com.example.smartlock.model.auth.AuthResponse
import com.example.smartlock.model.auth.RefreshTokenRequest
import com.example.smartlock.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshApi {
    @POST("api/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<AuthResponse>
}