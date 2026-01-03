package com.example.smartlock.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class OtpResponse(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)