package com.reflection.thecampus.data.model

import androidx.annotation.Keep

@Keep
data class GroupChatMessage(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val senderId: String = "",
    val senderName: String = "",
    val courseId: String = "",
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, String> = emptyMap() // userId -> emoji
)
