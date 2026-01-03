package com.example.smartlock.model.door

data class CreateDoorRequest(
    val name: String,
    val doorCode: String,
    val mqttTopicPrefix: String,
    val macAddress: String
)
