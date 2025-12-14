package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.reflection.thecampus.data.model.GamificationData
import timber.log.Timber

class DashboardFragment : Fragment() {

    private lateinit var tvLevel: TextView
    private lateinit var tvXP: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressXP: LinearProgressIndicator
    private lateinit var tvAchievementsProgress: TextView
    private lateinit var tvStatsPreview: TextView
    private lateinit var tvTask1Status: TextView
    private lateinit var tvTask2Status: TextView
    private lateinit var tvTask3Status: TextView

    private lateinit var cardLeaderboard: LinearLayout
    private lateinit var cardAchievements: LinearLayout
    private lateinit var cardStats: LinearLayout

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var gamificationListener: ValueEventListener? = null

    // Level XP requirements (cumulative)
    private val LEVEL_XP_REQUIREMENTS = intArrayOf(
        0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700,        // Levels 1-10
        3250, 3850, 4500, 5200, 5950, 6750, 7600, 8500, 9450, 10450, // Levels 11-20
        11500, 12600, 13750, 14950, 16200, 17500, 18850, 20250, 21700, 23200, // Levels 21-30
        24750, 26350, 28000, 29700, 31450, 33250, 35100, 37000, 38950, 40950, // Levels 31-40
        43000, 45100, 47250, 49450, 51700, 54000, 56350, 58750, 61200, 63700  // Levels 41-50
    )

    // Total number of achievements (from guide)
    private val TOTAL_ACHIEVEMENTS = 15

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize views
        tvLevel = view.findViewById(R.id.tvLevel)
        tvXP = view.findViewById(R.id.tvXP)
        tvPoints = view.findViewById(R.id.tvPoints)
        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak)
        tvAccuracy = view.findViewById(R.id.tvAccuracy)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        progressXP = view.findViewById(R.id.progressXP)
        tvAchievementsProgress = view.findViewById(R.id.tvAchievementsProgress)
        tvStatsPreview = view.findViewById(R.id.tvStatsPreview)
        tvTask1Status = view.findViewById(R.id.tvTask1Status)
        tvTask2Status = view.findViewById(R.id.tvTask2Status)
        tvTask3Status = view.findViewById(R.id.tvTask3Status)

        cardLeaderboard = view.findViewById(R.id.cardLeaderboard)
        cardAchievements = view.findViewById(R.id.cardAchievements)
        cardStats = view.findViewById(R.id.cardStats)

        setupCardListeners()
        loadGamificationData()

        return view
    }

    private fun setupCardListeners() {
        cardLeaderboard.setOnClickListener {
            startActivity(android.content.Intent(context, LeaderboardActivity::class.java))
        }

        cardAchievements.setOnClickListener {
            Toast.makeText(context, "Achievements - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        cardStats.setOnClickListener {
            Toast.makeText(context, "Statistics - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGamificationData() {
        val userId = auth.currentUser?.uid ?: return

        gamificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(GamificationData::class.java) ?: GamificationData()
                updateUI(data)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error loading gamification data: ${error.message}")
            }
        }

        database.getReference("users/$userId/gamification")
            .addValueEventListener(gamificationListener!!)
    }

    private fun updateUI(data: GamificationData) {
        // Update level (show just the number)
        tvLevel.text = data.level.toString()
        
        val currentLevelXP = if (data.level > 1) LEVEL_XP_REQUIREMENTS[data.level - 1] else 0
        val nextLevelXP = if (data.level < 50) LEVEL_XP_REQUIREMENTS[data.level] else LEVEL_XP_REQUIREMENTS[49]
        val xpInCurrentLevel = data.xp - currentLevelXP
        val xpNeededForNextLevel = nextLevelXP - currentLevelXP
        
        tvXP.text = "$xpInCurrentLevel / $xpNeededForNextLevel XP"
        
        // Update progress bar and percentage
        val progress = if (xpNeededForNextLevel > 0) {
            ((xpInCurrentLevel.toFloat() / xpNeededForNextLevel.toFloat()) * 100).toInt()
        } else {
            100
        }
        progressXP.progress = progress
        tvProgressPercent.text = "$progress%"

        // Update points
        tvPoints.text = data.points.toString()

        // Update streak (show just the number)
        tvCurrentStreak.text = data.currentStreak.toString()

        // Update accuracy
        val accuracy = if (data.stats.averageAccuracy > 0) {
            String.format("%.0f%%", data.stats.averageAccuracy)
        } else {
            "0%"
        }
        tvAccuracy.text = accuracy

        // Update achievements progress
        val unlockedCount = data.achievements.size
        tvAchievementsProgress.text = "$unlockedCount / $TOTAL_ACHIEVEMENTS unlocked"

        // Update stats preview
        val testsText = if (data.stats.totalTests == 1) {
            "${data.stats.totalTests} test completed"
        } else {
            "${data.stats.totalTests} tests completed"
        }
        tvStatsPreview.text = testsText

        // Update daily tasks status
        // Task 1: Complete a test (check if user completed a test today)
        val testsToday = 0 // TODO: Track daily test completion
        tvTask1Status.text = "$testsToday/1"

        // Task 2: Login streak (always completed if user is logged in)
        tvTask2Status.text = "✓"

        // Task 3: Study time (placeholder)
        val studyMinutesToday = 0 // TODO: Track study time
        tvTask3Status.text = "$studyMinutesToday/30"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gamificationListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.getReference("users/$userId/gamification").removeEventListener(it)
            }
        }
    }
}
