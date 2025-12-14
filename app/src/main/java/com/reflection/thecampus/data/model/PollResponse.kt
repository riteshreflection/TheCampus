package com.reflection.thecampus.data.model

data class PollResponse(
    val selectedOptions: List<String> = emptyList(),
    val submittedAt: Long = 0L,
    val isCorrect: Boolean? = null // For quiz polls
)
