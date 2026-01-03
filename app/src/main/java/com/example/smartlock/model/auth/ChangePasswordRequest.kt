package com.example.smartlock.model.auth

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
