package com.example.smartlock

import com.example.smartlock.model.AuthResponse
import com.example.smartlock.model.LoginRequest
import com.example.smartlock.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}

