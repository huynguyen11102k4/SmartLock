package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "door_records")
data class DoorRecord(
    @PrimaryKey
    val id: String,
    val event: String?=null,
    val method: String?=null,
    val occurredAt: String?= null
)