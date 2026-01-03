package com.example.smartlock.model.auth

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)
