package com.example.smartlock.model.auth

data class LinkOAuthRequest(
    val provider: String,
    val token: String
)
