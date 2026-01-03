package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "door_shares")
data class DoorShare(
    @PrimaryKey val id: String,
    val doorId: String,
    val userId: String,
    val permission: String,
    val validFrom: String? = null,
    val validTo: String? = null
)