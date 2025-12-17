package com.example.smartlock.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val user: User,
    val token: String
)
