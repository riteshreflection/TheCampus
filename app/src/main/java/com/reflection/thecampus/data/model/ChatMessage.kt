package com.reflection.thecampus.data.model

data class ChatMessage(
    val text: String = "",
    val timestamp: Long = 0,
    val senderId: String = "",
    val isRead: Boolean = false,
    val courseName: String = ""
)
