package com.example.smartlock.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String?
)