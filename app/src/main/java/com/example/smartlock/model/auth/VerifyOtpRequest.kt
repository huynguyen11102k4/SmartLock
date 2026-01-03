package com.example.smartlock.model.auth

data class VerifyOtpRequest(
    val email: String,
    val otp: String,
    val newPassword: String? = null
)
