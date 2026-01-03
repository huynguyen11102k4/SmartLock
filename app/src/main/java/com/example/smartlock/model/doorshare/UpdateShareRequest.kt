package com.example.smartlock.model.doorshare

data class UpdateShareRequest(
    val permission: Int,
    val validFrom: String? = null,
    val validTo: String? = null
)
