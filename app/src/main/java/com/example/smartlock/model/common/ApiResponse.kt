package com.example.smartlock.model.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val value: T? = null,
    val errorMessage: String? = null
)
