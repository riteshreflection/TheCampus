package com.reflection.thecampus.data.model

data class CourseChatPreview(
    val courseId: String = "",
    val courseName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSender: String = "",
    val courseImageUrl: String = ""
)
