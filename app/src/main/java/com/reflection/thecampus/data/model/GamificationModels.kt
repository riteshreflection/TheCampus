package com.reflection.thecampus.data.model

/**
 * Main gamification data structure stored in Firebase
 * Path: users/{userId}/gamification
 */
data class GamificationData(
    val points: Int = 0,                    // Total points earned
    val level: Int = 1,                     // Current level (1-50)
    val xp: Int = 0,                        // Total XP accumulated
    val currentStreak: Int = 0,             // Current login streak (days)
    val longestStreak: Int = 0,             // Longest streak ever achieved
    val lastLoginDate: Long = 0,            // Timestamp of last login
    val achievements: List<String> = emptyList(),  // List of unlocked achievement IDs
    val badges: Map<String, Badge> = emptyMap(),   // Unlocked badges
    val stats: Stats = Stats()              // User statistics
)

/**
 * Badge data structure
 */
data class Badge(
    val unlockedAt: Long = 0,
    val rarity: String = "common"  // common, rare, epic, legendary
)

/**
 * User statistics for gamification
 */
data class Stats(
    val totalTests: Int = 0,                // Total tests completed
    val perfectScores: Int = 0,             // Number of 100% scores
    val averageAccuracy: Double = 0.0,      // Average test accuracy
    val totalStudyTime: Long = 0,           // Total study time in milliseconds
    val testsCompletedToday: Int = 0,       // Tests completed today
    val dailyHighScore: Double = 0.0,       // Highest score today
    val dailyStudyTime: Long = 0,           // Study time today
    val lastTestDate: Long = 0              // Timestamp of last test
)

/**
 * Achievement structure
 */
data class Achievement(
    val id: String,                         // Unique identifier (e.g., "first_steps")
    val title: String,                      // Display title
    val description: String,                // Description text
    val icon: String,                       // Emoji or icon identifier
    val requirement: Requirement,           // Unlock requirement
    val points: Int,                        // XP awarded on unlock
    val rarity: String,                     // common, rare, epic, legendary
    val category: String                    // starter, performance, engagement, social
)

/**
 * Achievement requirement
 */
data class Requirement(
    val type: String,                       // test_count, perfect_score, streak, accuracy, speed, review, rank
    val value: Int,                         // Target value
    val comparison: String = "gte"          // gte (>=), lte (<=), eq (==)
)

/**
 * Result of XP award operation
 */
data class XPAwardResult(
    val newXP: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val newAchievements: List<Achievement>
)

/**
 * Result of streak update operation
 */
data class StreakResult(
    val currentStreak: Int,
    val streakIncreased: Boolean,
    val newAchievements: List<Achievement>
)

/**
 * Result of test completion
 */
data class TestCompletionResult(
    val points: Int,
    val xp: Int,
    val newAchievements: List<Achievement>
)

/**
 * User rank information
 */
data class RankResult(
    val rank: Int,
    val totalUsers: Int,
    val percentile: Double
)

/**
 * Leaderboard entry for a single user
 */
data class LeaderboardEntry(
    val userId: String = "",
    val displayName: String = "Anonymous",
    val email: String? = null,
    val avatar: String? = null,
    val points: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalTests: Int = 0,
    val averageScore: Double = 0.0,
    val totalTimeSpent: Long = 0L,
    val coursesCompleted: Int = 0,
    val lastActive: Long = System.currentTimeMillis(),
    val rank: Int = 0
)

/**
 * Container for all leaderboard types
 */
data class LeaderboardsData(
    val global: List<LeaderboardEntry> = emptyList(),
    val weekly: List<LeaderboardEntry> = emptyList(),
    val streak: List<LeaderboardEntry> = emptyList(),
    val accuracy: List<LeaderboardEntry> = emptyList(),
    val testMasters: List<LeaderboardEntry> = emptyList(),
    val levelRanking: List<LeaderboardEntry> = emptyList()
)
