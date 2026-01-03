package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passcodes")
data class Passcode(
    @PrimaryKey val id: String,
    val code: String? = "",
    val type: String? = "OneTime",
    val validFrom: String? = null,
    val validTo: String? = null,
    val isActive: Boolean = true
)