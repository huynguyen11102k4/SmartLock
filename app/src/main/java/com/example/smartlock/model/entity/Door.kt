package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "doors")
data class Door(
    @PrimaryKey val id: String,
    val ownerId: String? = null,
    val doorCode: String? = null,
    val name: String,
    val state: String? = "Unknown",
    val mqttTopicPrefix: String? = null,
    val macAddress: String? = null,
    val battery: Int = 0,
    val lastSyncAt: String? = null,
    val lastSyncRequestedAt: String? = null,
    val permission: Int = 0, // 0: Owner, 1: Admin, 2: User, 3: TimeRestricted
    val validFrom: String? = null,
    val validTo: String? = null
)