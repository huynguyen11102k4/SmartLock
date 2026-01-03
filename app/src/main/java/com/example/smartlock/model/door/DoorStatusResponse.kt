package com.example.smartlock.model.door

data class DoorStatusResponse(
    val doorId: String,
    val state: String,
    val battery: Int? = null,
    val lastSyncAt: String? = null
)
