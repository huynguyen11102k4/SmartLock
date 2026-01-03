package com.example.smartlock.model.passcode

data class UpdatePasscodeRequest(
    val code: String,
    val type: Int,
    val validFrom: String? = null,
    val validTo: String? = null
)
