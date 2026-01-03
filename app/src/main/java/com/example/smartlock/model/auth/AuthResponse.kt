package com.example.smartlock.model.auth

import com.example.smartlock.model.entity.User

data class AuthResponse(
    val userId: String,
    val provider: String,
    val accessToken: String,
    val refreshToken: String,
)
