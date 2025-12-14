package com.reflection.thecampus

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.reflection.thecampus.data.model.GamificationData
import com.reflection.thecampus.data.model.LeaderboardEntry
import com.reflection.thecampus.data.model.LeaderboardsData
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerLayout: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var emptyState: View
    private lateinit var tvGlobalRank: TextView
    private lateinit var tvGlobalPoints: TextView
    private lateinit var tvStreakRank: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var tvAccuracyRank: TextView
    private lateinit var tvAccuracyPercent: TextView
    private lateinit var tvLeaderboardTitle: TextView
    private lateinit var tvLeaderboardSubtitle: TextView
    private lateinit var tvStudentCount: TextView

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var leaderboardsData: LeaderboardsData? = null
    private var currentUserData: GamificationData? = null

    private lateinit var adapter: LeaderboardAdapter
    private var currentMetricType = LeaderboardAdapter.MetricType.POINTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
        
        setContentView(R.layout.activity_leaderboard)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupTabs()
        loadData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        shimmerLayout = findViewById(R.id.shimmerLayout)
        emptyState = findViewById(R.id.emptyState)
        tvGlobalRank = findViewById(R.id.tvGlobalRank)
        tvGlobalPoints = findViewById(R.id.tvGlobalPoints)
        tvStreakRank = findViewById(R.id.tvStreakRank)
        tvStreakDays = findViewById(R.id.tvStreakDays)
        tvAccuracyRank = findViewById(R.id.tvAccuracyRank)
        tvAccuracyPercent = findViewById(R.id.tvAccuracyPercent)
        tvLeaderboardTitle = findViewById(R.id.tvLeaderboardTitle)
        tvLeaderboardSubtitle = findViewById(R.id.tvLeaderboardSubtitle)
        tvStudentCount = findViewById(R.id.tvStudentCount)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter(emptyList(), auth.currentUser?.uid, currentMetricType)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("🏆 Global").setIcon(null))
        tabLayout.addTab(tabLayout.newTab().setText("📅 Weekly"))
        tabLayout.addTab(tabLayout.newTab().setText("⚡ Streak"))
        tabLayout.addTab(tabLayout.newTab().setText("🎯 Accuracy"))
        tabLayout.addTab(tabLayout.newTab().setText("🧪 Tests"))
        tabLayout.addTab(tabLayout.newTab().setText("🎖️ Level"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showLeaderboard(LeaderboardType.GLOBAL)
                    1 -> showLeaderboard(LeaderboardType.WEEKLY)
                    2 -> showLeaderboard(LeaderboardType.STREAK)
                    3 -> showLeaderboard(LeaderboardType.ACCURACY)
                    4 -> showLeaderboard(LeaderboardType.TESTS)
                    5 -> showLeaderboard(LeaderboardType.LEVEL)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadData() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Fetch current user data and leaderboard data in parallel
                val userId = auth.currentUser?.uid
                
                val currentUserDeferred = async {
                    if (userId != null) {
                        database.getReference("users/$userId/gamification").get().await()
                            .getValue(GamificationData::class.java)
                    } else null
                }
                
                val leaderboardDeferred = async {
                    fetchLeaderboardData()
                }
                
                // Wait for both to complete
                currentUserData = currentUserDeferred.await()
                val entries = leaderboardDeferred.await()
                
                leaderboardsData = generateLeaderboards(entries)

                // Update rank cards
                updateRankCards()

                // Show default leaderboard
                showLeaderboard(LeaderboardType.GLOBAL)

                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
            } catch (e: Exception) {
                Timber.e(e, "Error loading leaderboard data")
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun fetchLeaderboardData(): List<LeaderboardEntry> {
        val entries = mutableListOf<LeaderboardEntry>()

        try {
            // Fetch all users' gamification data
            val usersSnapshot = database.getReference("users").get().await()
            
            // Collect all user IDs first
            val userIds = mutableListOf<String>()
            val gamificationDataMap = mutableMapOf<String, GamificationData>()
            
            usersSnapshot.children.forEach { userSnapshot ->
                val userId = userSnapshot.key ?: return@forEach
                val gamificationSnapshot = userSnapshot.child("gamification")

                if (gamificationSnapshot.exists()) {
                    val gamification = gamificationSnapshot.getValue(GamificationData::class.java)
                    if (gamification != null && gamification.points > 0) {
                        userIds.add(userId)
                        gamificationDataMap[userId] = gamification
                    }
                }
            }
            
            // Batch fetch all user profiles in one request
            val profilesSnapshot = database.getReference("userProfiles").get().await()
            val profilesMap = mutableMapOf<String, Pair<String, String?>>()
            
            profilesSnapshot.children.forEach { profileSnapshot ->
                val userId = profileSnapshot.key ?: return@forEach
                val fullName = profileSnapshot.child("fullName").getValue(String::class.java)
                    ?: profileSnapshot.child("name").getValue(String::class.java)
                    ?: "User"
                val email = profileSnapshot.child("email").getValue(String::class.java)
                profilesMap[userId] = Pair(fullName, email)
            }
            
            // Create entries with cached profile data
            userIds.forEach { userId ->
                val gamification = gamificationDataMap[userId] ?: return@forEach
                val (displayName, email) = profilesMap[userId] ?: Pair("User ${userId.take(8)}", null)
                
                entries.add(
                    LeaderboardEntry(
                        userId = userId,
                        displayName = displayName,
                        email = email,
                        points = gamification.points,
                        level = gamification.level,
                        currentStreak = gamification.currentStreak,
                        longestStreak = gamification.longestStreak,
                        totalTests = gamification.stats.totalTests,
                        averageScore = gamification.stats.averageAccuracy,
                        totalTimeSpent = gamification.stats.totalStudyTime,
                        lastActive = gamification.lastLoginDate
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching leaderboard data")
        }

        return entries
    }

    private fun generateLeaderboards(allEntries: List<LeaderboardEntry>): LeaderboardsData {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        return LeaderboardsData(
            global = allEntries
                .sortedByDescending { it.points }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) },

            weekly = allEntries
                .filter { it.lastActive >= weekAgo }
                .sortedByDescending { it.points }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) },

            streak = allEntries
                .sortedByDescending { it.currentStreak }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) },

            accuracy = allEntries
                .filter { it.totalTests >= 5 }
                .sortedByDescending { it.averageScore }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) },

            testMasters = allEntries
                .sortedByDescending { it.totalTests }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) },

            levelRanking = allEntries
                .sortedByDescending { it.level }
                .take(50)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
        )
    }

    private fun updateRankCards() {
        val userId = auth.currentUser?.uid ?: return
        val data = leaderboardsData ?: return

        // Global rank
        val globalRank = data.global.indexOfFirst { it.userId == userId } + 1
        if (globalRank > 0) {
            tvGlobalRank.text = "#$globalRank"
            tvGlobalPoints.text = currentUserData?.points?.toString() ?: "0"
        }

        // Streak rank
        val streakRank = data.streak.indexOfFirst { it.userId == userId } + 1
        if (streakRank > 0) {
            tvStreakRank.text = "#$streakRank"
            tvStreakDays.text = "${currentUserData?.currentStreak ?: 0}🔥"
        }

        // Accuracy rank
        val accuracyRank = data.accuracy.indexOfFirst { it.userId == userId } + 1
        if (accuracyRank > 0 && (currentUserData?.stats?.totalTests ?: 0) >= 5) {
            tvAccuracyRank.text = "#$accuracyRank"
            tvAccuracyPercent.text = String.format("%.1f%%", currentUserData?.stats?.averageAccuracy ?: 0.0)
        } else {
            tvAccuracyRank.text = "#N/A"
            tvAccuracyPercent.text = String.format("%.1f%%", currentUserData?.stats?.averageAccuracy ?: 0.0)
        }
    }

    private fun showLeaderboard(type: LeaderboardType) {
        val data = leaderboardsData ?: return

        val (entries, title, subtitle, metricType) = when (type) {
            LeaderboardType.GLOBAL -> {
                Tuple4(
                    data.global,
                    "Global Leaderboard",
                    "Top performers by total points earned",
                    LeaderboardAdapter.MetricType.POINTS
                )
            }
            LeaderboardType.WEEKLY -> {
                Tuple4(
                    data.weekly,
                    "Weekly Leaderboard",
                    "Active users in the last 7 days",
                    LeaderboardAdapter.MetricType.POINTS
                )
            }
            LeaderboardType.STREAK -> {
                Tuple4(
                    data.streak,
                    "Streak Leaderboard",
                    "Top users by current login streak",
                    LeaderboardAdapter.MetricType.STREAK
                )
            }
            LeaderboardType.ACCURACY -> {
                Tuple4(
                    data.accuracy,
                    "Accuracy Leaderboard",
                    "Top performers by average test accuracy (min 5 tests)",
                    LeaderboardAdapter.MetricType.ACCURACY
                )
            }
            LeaderboardType.TESTS -> {
                Tuple4(
                    data.testMasters,
                    "Test Masters",
                    "Top users by total tests completed",
                    LeaderboardAdapter.MetricType.TESTS
                )
            }
            LeaderboardType.LEVEL -> {
                Tuple4(
                    data.levelRanking,
                    "Level Ranking",
                    "Top users by level achieved",
                    LeaderboardAdapter.MetricType.LEVEL
                )
            }
        }

        currentMetricType = metricType
        adapter.updateData(entries, metricType)

        tvLeaderboardTitle.text = title
        tvLeaderboardSubtitle.text = subtitle
        tvStudentCount.text = "${entries.size}"

        if (entries.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private enum class LeaderboardType {
        GLOBAL, WEEKLY, STREAK, ACCURACY, TESTS, LEVEL
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
