package com.reflection.thecampus.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "test_attempts",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["testId"]),
        Index(value = ["isSynced"])
    ]
)
data class TestAttemptEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val testTitle: String,
    val studentId: String,
    val submittedAt: Long,
    val timeTaken: Long,
    val score: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val answersJson: String, // JSON serialized answers
    val isSynced: Boolean = false // For offline submission tracking
)
