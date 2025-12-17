package com.example.smartlock.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "records")
data class Record(
    @PrimaryKey val id: String = "",
    val doorId: String = "",
    val timestamp: Date = Date(),
    val event: String = "",
    val method: String = "",
    val detail: String = "",
    val state: String = "locked",
    val sourceMqttMessage: String = ""
)
