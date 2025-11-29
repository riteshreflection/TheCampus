package com.reflection.thecampus

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.data.model.DailyPerformance
import com.reflection.thecampus.data.model.TestAttempt
import com.reflection.thecampus.data.model.UserAnalytics
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var radarChart: RadarChart
    
    private lateinit var tvTotalTests: TextView
    private lateinit var tvAvgScore: TextView
    private lateinit var tvStudyTime: TextView
    private lateinit var tvAvgAccuracy: TextView
    
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    
    private val testAttempts = mutableListOf<TestAttempt>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        // Set status bar color
        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        setupToolbar()
        initializeViews()
        loadTestAttempts()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        radarChart = findViewById(R.id.radarChart)
        
        tvTotalTests = findViewById(R.id.tvTotalTests)
        tvAvgScore = findViewById(R.id.tvAvgScore)
        tvStudyTime = findViewById(R.id.tvStudyTime)
        tvAvgAccuracy = findViewById(R.id.tvAvgAccuracy)
        
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
    }

    private fun loadTestAttempts() {
        progressBar.visibility = View.VISIBLE
        
        val database = FirebaseDatabase.getInstance()
        database.getReference("testAttempts")
            .orderByChild("studentId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    testAttempts.clear()
                    
                    for (child in snapshot.children) {
                        val attempt = child.getValue(TestAttempt::class.java)
                        if (attempt != null) {
                            testAttempts.add(attempt)
                        }
                    }
                    
                    progressBar.visibility = View.GONE
                    
                    if (testAttempts.isEmpty()) {
                        showEmptyState()
                    } else {
                        displayAnalytics()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    showEmptyState()
                }
            })
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
    }

    private fun displayAnalytics() {
        calculateAndDisplayStatistics()
        setupPerformanceTrendChart()
        setupAnswerDistributionChart()
        setupWeeklyActivityChart()
        setupPerformanceRadarChart()
        syncToFirebase()
    }

    private fun calculateAndDisplayStatistics() {
        val totalTests = testAttempts.size
        val averageScore = if (totalTests > 0) {
            testAttempts.sumOf { it.score } / totalTests
        } else 0.0
        
        val totalStudyTimeSeconds = testAttempts.sumOf { it.timeTaken }
        val totalStudyTimeHours = totalStudyTimeSeconds / 3600.0
        
        val averageAccuracy = if (totalTests > 0) {
            testAttempts.map { attempt ->
                val totalQuestions = attempt.correctCount + attempt.incorrectCount + attempt.unattemptedCount
                if (totalQuestions > 0) {
                    (attempt.correctCount.toDouble() / totalQuestions) * 100
                } else 0.0
            }.average()
        } else 0.0
        
        tvTotalTests.text = totalTests.toString()
        tvAvgScore.text = "${averageScore.roundToInt()}%"
        tvStudyTime.text = String.format("%.1fh", totalStudyTimeHours)
        tvAvgAccuracy.text = "${averageAccuracy.roundToInt()}%"
    }

    private fun setupPerformanceTrendChart() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val recentAttempts = testAttempts.filter { it.submittedAt >= thirtyDaysAgo }
            .sortedBy { it.submittedAt }
        
        if (recentAttempts.isEmpty()) {
            lineChart.visibility = View.GONE
            return
        }
        
        val scoreEntries = mutableListOf<Entry>()
        val accuracyEntries = mutableListOf<Entry>()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        recentAttempts.forEachIndexed { index, attempt ->
            val totalQuestions = attempt.correctCount + attempt.incorrectCount + attempt.unattemptedCount
            val accuracy = if (totalQuestions > 0) {
                (attempt.correctCount.toDouble() / totalQuestions) * 100
            } else 0.0
            
            scoreEntries.add(Entry(index.toFloat(), attempt.score.toFloat()))
            accuracyEntries.add(Entry(index.toFloat(), accuracy.toFloat()))
        }
        
        val scoreDataSet = LineDataSet(scoreEntries, "Score").apply {
            color = ContextCompat.getColor(this@AnalyticsActivity, R.color.md_theme_light_primary)
            setCircleColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.md_theme_light_primary))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val accuracyDataSet = LineDataSet(accuracyEntries, "Accuracy").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val lineData = LineData(scoreDataSet, accuracyDataSet)
        lineChart.data = lineData
        
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.setTouchEnabled(true)
        lineChart.setScaleEnabled(false)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    private fun setupAnswerDistributionChart() {
        val correctTotal = testAttempts.sumOf { it.correctCount }
        val incorrectTotal = testAttempts.sumOf { it.incorrectCount }
        val unattemptedTotal = testAttempts.sumOf { it.unattemptedCount }
        
        val entries = listOf(
            PieEntry(correctTotal.toFloat(), "Correct"),
            PieEntry(incorrectTotal.toFloat(), "Incorrect"),
            PieEntry(unattemptedTotal.toFloat(), "Unattempted")
        )
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#F44336"),
                Color.parseColor("#9E9E9E")
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        
        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }
        
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.holeRadius = 40f
        pieChart.setDrawEntryLabels(false)
        pieChart.legend.isEnabled = true
        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupWeeklyActivityChart() {
        val dayMinutes = IntArray(7) { 0 }
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        testAttempts.forEach { attempt ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = attempt.submittedAt
            }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
            dayMinutes[dayOfWeek] += (attempt.timeTaken / 60).toInt()
        }
        
        val entries = dayMinutes.mapIndexed { index, minutes ->
            BarEntry(index.toFloat(), minutes.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Minutes Studied").apply {
            color = ContextCompat.getColor(this@AnalyticsActivity, R.color.md_theme_light_primary)
            valueTextSize = 10f
        }
        
        val barData = BarData(dataSet)
        barChart.data = barData
        
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayNames)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.granularity = 1f
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.legend.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupPerformanceRadarChart() {
        val totalTests = testAttempts.size.toDouble()
        if (totalTests == 0.0) {
            radarChart.visibility = View.GONE
            return
        }
        
        val avgAccuracy = testAttempts.map { attempt ->
            val total = attempt.correctCount + attempt.incorrectCount + attempt.unattemptedCount
            if (total > 0) (attempt.correctCount.toDouble() / total) * 100 else 0.0
        }.average()
        
        val avgScore = testAttempts.sumOf { it.score } / totalTests
        val avgSpeed = 100 - ((testAttempts.sumOf { it.timeTaken } / totalTests) / 60).coerceAtMost(100.0)
        val completionRate = testAttempts.map { attempt ->
            val total = attempt.correctCount + attempt.incorrectCount + attempt.unattemptedCount
            if (total > 0) ((attempt.correctCount + attempt.incorrectCount).toDouble() / total) * 100 else 0.0
        }.average()
        
        val entries = listOf(
            RadarEntry(avgAccuracy.toFloat()),
            RadarEntry(avgScore.toFloat()),
            RadarEntry(avgSpeed.toFloat()),
            RadarEntry(completionRate.toFloat())
        )
        
        val dataSet = RadarDataSet(entries, "Performance").apply {
            color = ContextCompat.getColor(this@AnalyticsActivity, R.color.md_theme_light_primary)
            fillColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.md_theme_light_primary)
            setDrawFilled(true)
            fillAlpha = 100
            lineWidth = 2f
            valueTextSize = 10f
        }
        
        val radarData = RadarData(dataSet)
        radarChart.data = radarData
        
        radarChart.description.isEnabled = false
        radarChart.webLineWidth = 1f
        radarChart.webColor = Color.LTGRAY
        radarChart.webLineWidthInner = 1f
        radarChart.webColorInner = Color.LTGRAY
        radarChart.webAlpha = 100
        radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Accuracy", "Score", "Speed", "Completion"))
        radarChart.yAxis.axisMinimum = 0f
        radarChart.yAxis.axisMaximum = 100f
        radarChart.animateXY(1000, 1000)
        radarChart.invalidate()
    }

    private fun syncToFirebase() {
        val analytics = UserAnalytics(
            userId = currentUserId,
            totalTests = testAttempts.size,
            averageScore = if (testAttempts.isNotEmpty()) testAttempts.sumOf { it.score } / testAttempts.size else 0.0,
            totalStudyTimeSeconds = testAttempts.sumOf { it.timeTaken },
            averageAccuracy = if (testAttempts.isNotEmpty()) {
                testAttempts.map { attempt ->
                    val total = attempt.correctCount + attempt.incorrectCount + attempt.unattemptedCount
                    if (total > 0) (attempt.correctCount.toDouble() / total) * 100 else 0.0
                }.average()
            } else 0.0,
            correctAnswers = testAttempts.sumOf { it.correctCount },
            incorrectAnswers = testAttempts.sumOf { it.incorrectCount },
            unattemptedAnswers = testAttempts.sumOf { it.unattemptedCount },
            lastUpdated = System.currentTimeMillis()
        )
        
        FirebaseDatabase.getInstance()
            .getReference("userAnalytics")
            .child(currentUserId)
            .setValue(analytics)
    }
}
