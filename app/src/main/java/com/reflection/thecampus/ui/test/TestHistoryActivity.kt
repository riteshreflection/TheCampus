package com.reflection.thecampus.ui.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.TestAttempt

class TestHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: TestHistoryAdapter
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_history)

        // Set status bar color
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data
        
        // Handle status bar icons
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rvTestHistory = findViewById<RecyclerView>(R.id.rvTestHistory)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvNoHistory = findViewById<TextView>(R.id.tvNoHistory)

        rvTestHistory.layoutManager = LinearLayoutManager(this)
        adapter = TestHistoryAdapter(emptyList()) { attempt ->
            val intent = Intent(this, TestResultActivity::class.java)
            intent.putExtra("ATTEMPT_ID", attempt.id)
            // Pass other data as fallback or for immediate display while loading
            intent.putExtra("SCORE", attempt.score)
            intent.putExtra("CORRECT", attempt.correctCount)
            intent.putExtra("INCORRECT", attempt.incorrectCount)
            intent.putExtra("UNATTEMPTED", attempt.unattemptedCount)
            intent.putExtra("TIME_TAKEN", attempt.timeTaken)
            startActivity(intent)
        }
        rvTestHistory.adapter = adapter

        loadHistory(progressBar, tvNoHistory, rvTestHistory)
    }

    private fun loadHistory(progressBar: ProgressBar, tvNoHistory: TextView, rvTestHistory: RecyclerView) {
        val userId = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        
        // Query testAttempts where studentId matches current user
        database.getReference("testAttempts")
            .orderByChild("studentId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val attempts = mutableListOf<TestAttempt>()
                    
                    for (child in snapshot.children) {
                        val attempt = child.getValue(TestAttempt::class.java)
                        if (attempt != null) {
                            // Manually set ID if missing (it should be key)
                            val attemptWithId = if (attempt.id.isEmpty()) attempt.copy(id = child.key ?: "") else attempt
                            attempts.add(attemptWithId)
                        }
                    }
                    
                    // Sort by date descending
                    attempts.sortByDescending { it.submittedAt }
                    
                    if (attempts.isEmpty()) {
                        progressBar.visibility = View.GONE
                        tvNoHistory.visibility = View.VISIBLE
                        rvTestHistory.visibility = View.GONE
                    } else {
                        // Fetch test names for all attempts
                        fetchTestNames(attempts, progressBar, tvNoHistory, rvTestHistory)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    android.widget.Toast.makeText(this@TestHistoryActivity, "Failed to load history: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun fetchTestNames(attempts: List<TestAttempt>, progressBar: ProgressBar, tvNoHistory: TextView, rvTestHistory: RecyclerView) {
        val testsRef = database.getReference("tests")
        var loadedCount = 0
        val attemptsWithNames = mutableListOf<TestAttempt>()
        
        for (attempt in attempts) {
            if (attempt.testTitle.isNotEmpty()) {
                // Already has title
                attemptsWithNames.add(attempt)
                loadedCount++
                if (loadedCount == attempts.size) {
                    displayAttempts(attemptsWithNames, progressBar, tvNoHistory, rvTestHistory)
                }
            } else {
                // Fetch test name from Firebase
                testsRef.child(attempt.testId).child("title")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(titleSnapshot: DataSnapshot) {
                            val testName = titleSnapshot.getValue(String::class.java) ?: "Test ID: ${attempt.testId}"
                            attemptsWithNames.add(attempt.copy(testTitle = testName))
                            loadedCount++
                            if (loadedCount == attempts.size) {
                                displayAttempts(attemptsWithNames, progressBar, tvNoHistory, rvTestHistory)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Fallback to test ID
                            attemptsWithNames.add(attempt.copy(testTitle = "Test ID: ${attempt.testId}"))
                            loadedCount++
                            if (loadedCount == attempts.size) {
                                displayAttempts(attemptsWithNames, progressBar, tvNoHistory, rvTestHistory)
                            }
                        }
                    })
            }
        }
    }
    
    private fun displayAttempts(attempts: List<TestAttempt>, progressBar: ProgressBar, tvNoHistory: TextView, rvTestHistory: RecyclerView) {
        progressBar.visibility = View.GONE
        // Sort again to maintain order
        val sortedAttempts = attempts.sortedByDescending { it.submittedAt }
        tvNoHistory.visibility = View.GONE
        rvTestHistory.visibility = View.VISIBLE
        adapter.updateData(sortedAttempts)
    }
}
