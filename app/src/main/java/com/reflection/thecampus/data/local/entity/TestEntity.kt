package com.reflection.thecampus.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "tests",
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["isFree"]),
        Index(value = ["status"])
    ]
)
data class TestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val duration: Int, // in minutes
    val totalMarks: Int,
    val totalQuestions: Int,
    val status: String,
    val isFree: Boolean,
    val level: String,
    val createdAt: Long,
    val createdBy: String,
    val courseId: String,
    val explanationPdfUrl: String,
    val explanationVideoUrl: String,
    val maxAttempts: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
