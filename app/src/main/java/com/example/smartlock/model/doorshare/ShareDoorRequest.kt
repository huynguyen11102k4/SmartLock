package com.example.smartlock.model.doorshare

data class ShareDoorRequest(
    val userId: String,
    val permission: Int,
    val validFrom: String? = null,
    val validTo: String? = null
)
