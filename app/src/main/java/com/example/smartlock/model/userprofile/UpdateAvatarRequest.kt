package com.example.smartlock.model.userprofile

data class UpdateAvatarRequest(
    val avatarUrl: String,
    val isRandom: Boolean = false
)
