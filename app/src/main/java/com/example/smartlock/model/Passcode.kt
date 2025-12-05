package com.example.smartlock.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passcodes")
data class Passcode(
    @PrimaryKey val code: String,
    val doorId: String,
    val type: String,
    val validity: String = "",
    val status: String = "Active"
)