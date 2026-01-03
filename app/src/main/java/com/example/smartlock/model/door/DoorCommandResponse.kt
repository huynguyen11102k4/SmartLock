package com.example.smartlock.model.door

data class DoorCommandResponse(
    val success: Boolean,
    val message: String,
    val timestamp: String
)
