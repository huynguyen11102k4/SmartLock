package com.example.smartlock.model.auth

data class LoginRequest(
    val type: String,
    val email: String? = null,
    val password: String? = null,
    val token: String? = null
)
