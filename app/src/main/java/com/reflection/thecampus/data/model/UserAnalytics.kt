package com.reflection.thecampus.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserAnalytics(
    val userId: String = "",
    val totalTests: Int = 0,
    val averageScore: Double = 0.0,
    val totalStudyTimeSeconds: Long = 0,
    val averageAccuracy: Double = 0.0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val unattemptedAnswers: Int = 0,
    val weeklyActivity: Map<String, Int> = emptyMap(), // Day -> Minutes
    val last30DaysData: List<DailyPerformance> = emptyList(),
    val lastUpdated: Long = 0
) : Parcelable

@Parcelize
data class DailyPerformance(
    val date: String = "",
    val score: Double = 0.0,
    val accuracy: Double = 0.0,
    val timestamp: Long = 0
) : Parcelable
