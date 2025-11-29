package com.reflection.thecampus.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "courses",
    indices = [
        Index(value = ["isEnrolled"]),
        Index(value = ["status"]),
        Index(value = ["cachedAt"])
    ]
)
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val level: String,
    val type: String,
    val hasVideoExplanation: Boolean,
    val price: Double,
    val discount: Int,
    val thumbnailUrl: String,
    val startDate: String,
    val endDate: String,
    val totalLectures: Int,
    val totalTests: Int,
    val maxStudents: Int,
    val status: String,
    val createdAt: Long,
    val isEnrolled: Boolean = false, // Local flag for user enrollment
    val instructorIds: List<String> = emptyList(),
    val linkedTests: List<String> = emptyList(),
    val contentJson: String = "{}", // Store content as JSON string
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val cachedAt: Long = System.currentTimeMillis()
)
