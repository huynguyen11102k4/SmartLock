package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "door_commands")
data class DoorCommand(
    @PrimaryKey val id: String,
    val doorId: String,
    val commandType: String,
    val payload: String,
    val status: String,
    val sentAt: Long,
    val ackAt: Long? = null
)