package com.example.smartlock.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doors")
data class Door(
    @PrimaryKey val id: String,
    val name: String,
    val permission: String = "Permanent",
    val battery: Int = 100,
    val macAddress: String? = null,
    val mqttTopicPrefix: String,
    val masterPasscode: String? = null,
    val currentState: String = "locked"
)
