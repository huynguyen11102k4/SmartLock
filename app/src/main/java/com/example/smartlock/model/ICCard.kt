package com.example.smartlock.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ic_cards")
data class ICCard(
    @PrimaryKey val id: String,
    val doorId: String,
    val name: String,
    val status: String = "Active"
)
