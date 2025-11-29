package com.reflection.thecampus.ui.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
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
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
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
            loadTestAndAttempt(attemptId, progressBar)
        }
    }

    private fun loadTestAndAttempt(attemptId: String, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        
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
                    fetchTest(attempt.testId, attempt, progressBar)
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateTimeAnalysis(timeTakenMillis: Long) {
        val tvTimeTaken = findViewById<TextView>(R.id.tvTimeTaken)
        tvTimeTaken.text = formatTime(timeTakenMillis)
    }

    private fun fetchTest(testId: String, attempt: TestAttempt, progressBar: ProgressBar) {
        database.getReference("tests").child(testId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(testSnapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
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
                progressBar.visibility = View.GONE
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
