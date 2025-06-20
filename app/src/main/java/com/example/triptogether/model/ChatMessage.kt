package com.example.triptogether.model

data class ChatMessage(
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
