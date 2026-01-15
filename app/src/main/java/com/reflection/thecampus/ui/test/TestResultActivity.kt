package com.reflection.thecampus.ui.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Test
import com.reflection.thecampus.data.model.TestAttempt
import java.util.concurrent.TimeUnit

class TestResultActivity : AppCompatActivity() {

    private lateinit var adapter: ResultQuestionAdapter
    private lateinit var sectionAdapter: SectionResultAdapter
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_result)

        // Set status bar color
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data
        
        // Handle status bar icons
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvCorrect = findViewById<TextView>(R.id.tvCorrectCount)
        val tvIncorrect = findViewById<TextView>(R.id.tvIncorrectCount)
        val tvUnattempted = findViewById<TextView>(R.id.tvUnattemptedCount)
        
        // New Views
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val svMainContent = findViewById<View>(R.id.svMainContent)
        val btnBack = findViewById<View>(R.id.btnBack)
        
        btnBack.setOnClickListener { finish() }

        val rvQuestionAnalysis = findViewById<RecyclerView>(R.id.rvQuestionAnalysis)
        val rvSectionAnalysis = findViewById<RecyclerView>(R.id.rvSectionAnalysis)

        // Setup RecyclerViews
        rvQuestionAnalysis.layoutManager = LinearLayoutManager(this)
        rvQuestionAnalysis.isNestedScrollingEnabled = false 
        adapter = ResultQuestionAdapter(emptyList(), emptyMap())
        rvQuestionAnalysis.adapter = adapter

        rvSectionAnalysis.layoutManager = LinearLayoutManager(this)
        rvSectionAnalysis.isNestedScrollingEnabled = false
        sectionAdapter = SectionResultAdapter(emptyList())
        rvSectionAnalysis.adapter = sectionAdapter

        setupLeaderboard()

        // Get data from Intent
        val score = intent.getDoubleExtra("SCORE", 0.0)
        val correct = intent.getIntExtra("CORRECT", 0)
        val incorrect = intent.getIntExtra("INCORRECT", 0)
        val unattempted = intent.getIntExtra("UNATTEMPTED", 0)
        val attemptId = intent.getStringExtra("ATTEMPT_ID")

        // Display basic stats immediately
        tvScore.text = String.format("%.1f", score)
        tvCorrect.text = correct.toString()
        tvIncorrect.text = incorrect.toString()
        tvUnattempted.text = unattempted.toString()

        // Fetch detailed data if attemptId is present
        if (!attemptId.isNullOrEmpty()) {
            loadTestAndAttempt(attemptId, pbLoading, svMainContent)
        } else {
            // If no attempt ID (e.g. preview?), show content
            pbLoading.visibility = View.GONE
            svMainContent.visibility = View.VISIBLE
        }
    }

    private fun loadTestAndAttempt(attemptId: String, pbLoading: ProgressBar, svMainContent: View) {
        pbLoading.visibility = View.VISIBLE
        svMainContent.visibility = View.GONE
        
        // 1. Fetch TestAttempt
        database.getReference("testAttempts").child(attemptId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(attemptSnapshot: DataSnapshot) {
                val attempt = attemptSnapshot.getValue(TestAttempt::class.java)
                if (attempt != null) {
                    // Update stats if they were not passed correctly (e.g. from history)
                    findViewById<TextView>(R.id.tvScore).text = String.format("%.1f", attempt.score)
                    findViewById<TextView>(R.id.tvCorrectCount).text = attempt.correctCount.toString()
                    findViewById<TextView>(R.id.tvIncorrectCount).text = attempt.incorrectCount.toString()
                    findViewById<TextView>(R.id.tvUnattemptedCount).text = attempt.unattemptedCount.toString()

                    // Update Time Analysis
                    updateTimeAnalysis(attempt.timeTaken * 1000L) // Convert seconds to millis

                    // 2. Fetch Test
                    fetchTest(attempt.testId, attempt, pbLoading, svMainContent)
                    
                    // 3. Fetch Leaderboard
                    fetchLeaderboardData(attempt.testId)
                } else {
                    pbLoading.visibility = View.GONE
                    svMainContent.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                pbLoading.visibility = View.GONE
                svMainContent.visibility = View.VISIBLE
            }
        })
    }

    private fun updateTimeAnalysis(timeTakenMillis: Long) {
        val tvTimeTaken = findViewById<TextView>(R.id.tvTimeTaken)
        tvTimeTaken.text = formatTime(timeTakenMillis)
    }

    private fun fetchTest(testId: String, attempt: TestAttempt, pbLoading: ProgressBar, svMainContent: View) {
        database.getReference("tests").child(testId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(testSnapshot: DataSnapshot) {
                pbLoading.visibility = View.GONE
                svMainContent.visibility = View.VISIBLE
                val test = testSnapshot.getValue(Test::class.java)
                if (test != null) {
                    // Update Total Marks and Progress
                    val tvTotalMarks = findViewById<TextView>(R.id.tvTotalMarks)
                    val piScore = findViewById<CircularProgressIndicator>(R.id.piScore)
                    val tvAccuracy = findViewById<TextView>(R.id.tvAccuracy)
                    val tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)
                    val pbTime = findViewById<ProgressBar>(R.id.pbTime)

                    tvTotalMarks.text = test.totalMarks.toString()
                    
                    // Calculate percentage for progress
                    val percentage = if (test.totalMarks > 0) (attempt.score / test.totalMarks) * 100 else 0.0
                    piScore.progress = percentage.toInt().coerceAtLeast(0)

                    // Calculate Accuracy
                    val totalAttempted = attempt.correctCount + attempt.incorrectCount
                    val accuracy = if (totalAttempted > 0) (attempt.correctCount.toDouble() / totalAttempted) * 100 else 0.0
                    tvAccuracy.text = "Accuracy: ${String.format("%.1f", accuracy)}%"

                    // Update Total Time
                    val totalTimeMillis = test.duration * 60 * 1000L
                    tvTotalTime.text = formatTime(totalTimeMillis)
                    
                    // Fix: Convert attempt.timeTaken (seconds) to millis for percentage calculation
                    val timeTakenMillis = attempt.timeTaken * 1000L
                    val timePercentage = if (totalTimeMillis > 0) (timeTakenMillis.toDouble() / totalTimeMillis) * 100 else 0.0
                    pbTime.progress = timePercentage.toInt()

                    // Section Analysis
                    calculateSectionAnalysis(test, attempt)

                    // Question Analysis
                    val allQuestions = test.getAllQuestions()
                    val answersMap = attempt.getAnswersMap()
                    adapter.updateData(allQuestions, answersMap)
                    
                    // Setup Download PDF Button
                    val btnDownloadPdf = findViewById<MaterialButton>(R.id.btnDownloadPdf)
                    if (test.explanationPdfUrl.isNotEmpty()) {
                        btnDownloadPdf.visibility = View.VISIBLE
                        btnDownloadPdf.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(test.explanationPdfUrl)
                            startActivity(intent)
                        }
                    } else {
                        btnDownloadPdf.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TestResultActivity, "Loading Failed check you internet connection", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun calculateSectionAnalysis(test: Test, attempt: TestAttempt) {
        val answersMap = attempt.getAnswersMap()
        val sectionResults = mutableListOf<SectionResult>()

        test.sections.forEach { section ->
            var sectionScore = 0.0
            var sectionTotalMarks = 0.0
            var sectionCorrect = 0
            var sectionAttempted = 0

            section.questions.forEach { question ->
                sectionTotalMarks += question.marks
                
                val rawAnswer = answersMap[question.id]
                if (rawAnswer != null) {
                    sectionAttempted++
                    
                    // Convert raw answer to String format expected by validateAnswer
                    val answerStr = when (rawAnswer) {
                        is List<*> -> rawAnswer.joinToString(",") // For MSQ
                        is Number -> rawAnswer.toString() // For NAT
                        else -> rawAnswer.toString() // For MCQ
                    }

                    if (question.validateAnswer(answerStr)) {
                        sectionScore += question.marks
                        sectionCorrect++
                    } else {
                        sectionScore -= question.negativeMarks
                    }
                }
            }
            
            val accuracy = if (sectionAttempted > 0) (sectionCorrect.toDouble() / sectionAttempted) * 100 else 0.0
            
            sectionResults.add(SectionResult(
                title = section.title,
                score = sectionScore,
                totalMarks = sectionTotalMarks,
                accuracy = accuracy.toInt()
            ))
        }

        sectionAdapter.updateData(sectionResults)
    }

    // Leaderboard
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private var leaderboardData: com.reflection.thecampus.data.model.LeaderboardData? = null
    private var currentTestId: String = ""

    private fun setupLeaderboard() {
        val rvLeaderboard = findViewById<RecyclerView>(R.id.rvLeaderboard)
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutLeaderboard)
        
        leaderboardAdapter = LeaderboardAdapter()
        rvLeaderboard.layoutManager = LinearLayoutManager(this)
        rvLeaderboard.adapter = leaderboardAdapter
        rvLeaderboard.isNestedScrollingEnabled = false

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                updateLeaderboardUI(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun fetchLeaderboardData(testId: String) {
        currentTestId = testId
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        database.getReference("testAttempts")
            .orderByChild("testId")
            .equalTo(testId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    processLeaderboardData(snapshot, currentUserId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error silently or show toast
                }
            })
    }

    private fun processLeaderboardData(snapshot: DataSnapshot, currentUserId: String) {
        val userBestAttempts = mutableMapOf<String, com.reflection.thecampus.data.model.UserBestAttempt>()
        
        for (attemptSnapshot in snapshot.children) {
            val attempt = attemptSnapshot.getValue(TestAttempt::class.java) ?: continue
            val userId = attempt.studentId
            
            if (userId.isEmpty()) continue

            if (!userBestAttempts.containsKey(userId)) {
                userBestAttempts[userId] = com.reflection.thecampus.data.model.UserBestAttempt(
                    userId = userId,
                    userName = attempt.studentName ?: "Anonymous Student",
                    userAvatar = attempt.studentAvatar,
                    score = attempt.score,
                    timeTaken = attempt.timeTaken,
                    attemptCount = 1
                )
            } else {
                val existing = userBestAttempts[userId]!!
                val newAttemptCount = existing.attemptCount + 1
                
                // Check if this attempt is better (higher score, or same score with faster time)
                val isBetter = attempt.score > existing.score || 
                              (attempt.score == existing.score && attempt.timeTaken < existing.timeTaken)
                
                if (isBetter) {
                    userBestAttempts[userId] = existing.copy(
                        score = attempt.score,
                        timeTaken = attempt.timeTaken,
                        attemptCount = newAttemptCount
                    )
                } else {
                    userBestAttempts[userId] = existing.copy(attemptCount = newAttemptCount)
                }
            }
        }

        // Fetch User Profiles
        val userIds = userBestAttempts.keys.toList()
        if (userIds.isEmpty()) {
            finalizeLeaderboardData(userBestAttempts, currentUserId)
            return
        }

        val tasks = userIds.map { userId ->
            database.getReference("userProfiles").child(userId).get()
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                for (profileSnapshot in snapshots) {
                    val userId = profileSnapshot.key ?: continue
                    val fullName = profileSnapshot.child("name").getValue(String::class.java) 
                        ?: profileSnapshot.child("fullName").getValue(String::class.java)
                        ?: profileSnapshot.child("username").getValue(String::class.java) // try multiple fields
                    val avatar = profileSnapshot.child("avatar").getValue(String::class.java)
                        ?: profileSnapshot.child("profilePictureUrl").getValue(String::class.java)

                    if (userBestAttempts.containsKey(userId)) {
                        val current = userBestAttempts[userId]!!
                        userBestAttempts[userId] = current.copy(
                            userName = fullName ?: current.userName,
                            userAvatar = avatar ?: current.userAvatar
                        )
                    }
                }
                finalizeLeaderboardData(userBestAttempts, currentUserId)
            }
            .addOnFailureListener {
                // Determine if we should show basic data anyway or fail
                // For now, show with existing data (Anonymous)
                finalizeLeaderboardData(userBestAttempts, currentUserId)
            }
    }

    private fun finalizeLeaderboardData(
        userBestAttempts: Map<String, com.reflection.thecampus.data.model.UserBestAttempt>, 
        currentUserId: String
    ) {
        // Separate into first-timers and repeaters
        val firstTimers = mutableListOf<com.reflection.thecampus.data.model.LeaderboardEntry>()
        val repeaters = mutableListOf<com.reflection.thecampus.data.model.LeaderboardEntry>()
        
        // Comparator: Score Descending, then Time Ascending
        val comparator = Comparator<com.reflection.thecampus.data.model.LeaderboardEntry> { a, b ->
            if (a.score != b.score) {
                b.score.compareTo(a.score)
            } else {
                a.timeTaken.compareTo(b.timeTaken)
            }
        }

        userBestAttempts.values.forEach { user ->
            val entry = com.reflection.thecampus.data.model.LeaderboardEntry(
                userId = user.userId,
                userName = user.userName,
                userAvatar = user.userAvatar,
                score = user.score,
                timeTaken = user.timeTaken,
                rank = 0, // Will assign after sort
                isCurrentUser = user.userId == currentUserId,
                attemptCount = user.attemptCount
            )
            
            if (user.attemptCount == 1) {
                firstTimers.add(entry)
            } else {
                repeaters.add(entry)
            }
        }

        // Sort and assign ranks
        firstTimers.sortWith(comparator)
        repeaters.sortWith(comparator)

        val rankedFirstTimers = firstTimers.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
        val rankedRepeaters = repeaters.mapIndexed { index, entry -> entry.copy(rank = index + 1) }

        // Find current user entries for bottom display if needed
        val currentUserFirstTimer = rankedFirstTimers.find { it.isCurrentUser }
        val currentUserRepeater = rankedRepeaters.find { it.isCurrentUser }

        leaderboardData = com.reflection.thecampus.data.model.LeaderboardData(
            firstTimers = rankedFirstTimers,
            repeaters = rankedRepeaters,
            currentUserFirstTimerEntry = currentUserFirstTimer,
            currentUserRepeaterEntry = currentUserRepeater
        )

        // Update UI
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutLeaderboard)
        updateLeaderboardUI(tabLayout.selectedTabPosition)
        
        // Hide global loading if it was shown (it might have been hidden by fetchTest, but good to ensure)
    }

    private fun updateLeaderboardUI(tabPosition: Int) {
        val data = leaderboardData ?: return
        val listToShow = mutableListOf<Any>()
        val tvEmpty = findViewById<TextView>(R.id.tvLeaderboardEmpty)
        val rvLeaderboard = findViewById<RecyclerView>(R.id.rvLeaderboard)

        val sourceList = if (tabPosition == 0) data.firstTimers else data.repeaters
        val currentUserEntry = if (tabPosition == 0) data.currentUserFirstTimerEntry else data.currentUserRepeaterEntry

        if (sourceList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = getString(if (tabPosition == 0) R.string.leaderboard_empty_first_timers else R.string.leaderboard_empty_repeaters)
            rvLeaderboard.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvLeaderboard.visibility = View.VISIBLE

            // Add Top 20
            listToShow.addAll(sourceList.take(20))

            // Add Current User if outside Top 20
            if (currentUserEntry != null && currentUserEntry.rank > 20) {
                listToShow.add("SEPARATOR") // Marker for separator
                listToShow.add(currentUserEntry)
            }
        }

        leaderboardAdapter.submitList(listToShow)
    }

    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
