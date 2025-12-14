package com.reflection.thecampus.data.model

data class PollOption(
    val id: String = "",
    val text: String = "",
    val isCorrect: Boolean? = null // Only for quiz type, null for others
)
