package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ic_cards")
data class ICCard(
    @PrimaryKey val id: String,
    val cardUid: String? = "",
    val name: String? = "Thẻ IC",
    val isActive: Boolean = true
)