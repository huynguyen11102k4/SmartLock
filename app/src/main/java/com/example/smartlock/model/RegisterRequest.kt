package com.example.smartlock.model

data class RegisterRequest(
    val name: String,
    val password: String,
    val email: String
)
