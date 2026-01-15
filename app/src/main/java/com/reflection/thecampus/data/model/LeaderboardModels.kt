package com.reflection.thecampus.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Represents a user's best attempt for a specific test
 * Used internally for leaderboard calculation
 */
@Keep
data class UserBestAttempt(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val score: Double,
    val timeTaken: Long, // in seconds
    val attemptCount: Int
)

/**
 * Represents a single entry in the leaderboard
 */
@Keep
@Parcelize
data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val score: Double,
    val timeTaken: Long, // in seconds
    val rank: Int,
    val isCurrentUser: Boolean,
    val attemptCount: Int
) : Parcelable {
    
    /**
     * Check if this entry should use premium card design (top 3)
     */
    fun isPremium(): Boolean = rank in 1..3
    
    /**
     * Get the place text for premium cards
     */
    fun getPlaceText(): String = when (rank) {
        1 -> "1st Place"
        2 -> "2nd Place"
        3 -> "3rd Place"
        else -> ""
    }
}

/**
 * Container for complete leaderboard data
 */
@Keep
@Parcelize
data class LeaderboardData(
    val firstTimers: List<LeaderboardEntry>,
    val repeaters: List<LeaderboardEntry>,
    val currentUserFirstTimerEntry: LeaderboardEntry?,
    val currentUserRepeaterEntry: LeaderboardEntry?
) : Parcelable {
    
    /**
     * Get top 20 first-timers
     */
    fun getTop20FirstTimers(): List<LeaderboardEntry> = firstTimers.take(20)
    
    /**
     * Get top 20 repeaters
     */
    fun getTop20Repeaters(): List<LeaderboardEntry> = repeaters.take(20)
    
    /**
     * Check if current user is outside top 20 in first-timers
     */
    fun shouldShowCurrentUserAtBottomFirstTimers(): Boolean {
        return currentUserFirstTimerEntry != null && currentUserFirstTimerEntry.rank > 20
    }
    
    /**
     * Check if current user is outside top 20 in repeaters
     */
    fun shouldShowCurrentUserAtBottomRepeaters(): Boolean {
        return currentUserRepeaterEntry != null && currentUserRepeaterEntry.rank > 20
    }
}
