package com.example.smartlock.model.auth

data class RefreshTokenRequest(
    val refreshToken: String,
    val email: String
)
