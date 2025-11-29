package com.reflection.thecampus.ui.test

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.MaterialToolbar
import com.reflection.thecampus.R
import java.io.File

class TestInterfaceActivity : AppCompatActivity() {

    private val viewModel: TestViewModel by viewModels()
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var rvPalette: RecyclerView
    private lateinit var tvTimer: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var btnSubmit: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnMarkReview: Button
    private lateinit var btnClear: Button
    private lateinit var toolbar: MaterialToolbar

    private lateinit var questionAdapter: QuestionAdapter
    private lateinit var paletteAdapter: PaletteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_interface)

        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        val backgroundColor = typedValue.data
        window.statusBarColor = backgroundColor
        
        // Handle status bar icons (Dark icons for light background, Light icons for dark background)
        // We can check if the background color is light or dark
        val isLightBackground = androidx.core.graphics.ColorUtils.calculateLuminance(backgroundColor) > 0.5
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = isLightBackground

        initViews()
        setupObservers()
        
        val testId = intent.getStringExtra("TEST_ID")
        if (testId != null) {
            viewModel.loadTest(testId)
        } else {
            Toast.makeText(this, "Error: No Test ID provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun showTestInstructionsBottomSheet() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_test_instructions, null)
        bottomSheet.setContentView(view)
        bottomSheet.setCancelable(false)
        
        val tvTestTitle = view.findViewById<TextView>(R.id.tvTestTitle)
        val tvTestDetails = view.findViewById<TextView>(R.id.tvTestDetails)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val tvProgress = view.findViewById<TextView>(R.id.tvProgress)
        val btnStartTest = view.findViewById<Button>(R.id.btnStartTest)
        
        val test = viewModel.test.value
        if (test != null) {
            tvTestTitle.text = test.title
            tvTestDetails.text = "Total Questions: ${viewModel.getQuestions().size}\nDuration: ${test.duration} minutes\nTotal Marks: ${test.totalMarks}"
        }
        
        btnStartTest.setOnClickListener {
            // Disable button and show progress
            btnStartTest.isEnabled = false
            progressBar.visibility = android.view.View.VISIBLE
            tvProgress.visibility = android.view.View.VISIBLE
            
            // Pre-cache images
            preCacheImages(progressBar, tvProgress) {
                bottomSheet.dismiss()
                setupUI()
            }
        }
        
        bottomSheet.show()
    }
    
    private fun preCacheImages(progressBar: android.widget.ProgressBar, tvProgress: TextView, onComplete: () -> Unit) {
        val questions = viewModel.getQuestions()
        val imagesToCache = questions.filter { it.hasImage() }.map { it.questionImage }
        
        if (imagesToCache.isEmpty()) {
            onComplete()
            return
        }
        
        progressBar.max = imagesToCache.size
        progressBar.progress = 0
        tvProgress.text = "Downloading images... 0/${imagesToCache.size}"
        
        var cachedCount = 0
        
        imagesToCache.forEach { imageUrl ->
            com.bumptech.glide.Glide.with(this)
                .downloadOnly()
                .load(imageUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .listener(object : com.bumptech.glide.request.RequestListener<java.io.File> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<File?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        cachedCount++
                        updateProgress(progressBar, tvProgress, cachedCount, imagesToCache.size, onComplete)
                        return false
                    }

                    override fun onResourceReady(
                        resource: File,
                        model: Any,
                        target: Target<File?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        cachedCount++
                        updateProgress(progressBar, tvProgress, cachedCount, imagesToCache.size, onComplete)
                        return false
                    }
                })
                .preload()
        }
    }
    
    private fun updateProgress(progressBar: android.widget.ProgressBar, tvProgress: TextView, current: Int, total: Int, onComplete: () -> Unit) {
        runOnUiThread {
            progressBar.progress = current
            tvProgress.text = "Downloading images... $current/$total"
            
            if (current >= total) {
                tvProgress.text = "Ready to start!"
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onComplete()
                }, 500)
            }
        }
    }
    
    private fun setupUI() {
        // Called after images are cached
        setupAdapters()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        viewPager = findViewById(R.id.viewPager)
        rvPalette = findViewById(R.id.rvPalette)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        progressBar = findViewById(R.id.progressBar)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnMarkReview = findViewById(R.id.btnMarkReview)
        btnClear = findViewById(R.id.btnClear)
        toolbar = findViewById(R.id.toolbar)

        // Setup Toolbar
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size) // Simple icon for drawer
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END) // Palette is at END
        }

        // Setup Buttons
        btnNext.setOnClickListener { viewModel.nextQuestion() }
        btnPrevious.setOnClickListener { viewModel.previousQuestion() }
        btnMarkReview.setOnClickListener { 
            val current = viewModel.currentQuestionIndex.value ?: 0
            val question = viewModel.getQuestions().getOrNull(current)
            if (question != null) {
                viewModel.toggleMarkForReview(question.id)
                paletteAdapter.notifyItemChanged(current)
            }
        }
        btnClear.setOnClickListener {
            val current = viewModel.currentQuestionIndex.value ?: 0
            val question = viewModel.getQuestions().getOrNull(current)
            if (question != null) {
                viewModel.clearAnswer(question.id)
                questionAdapter.notifyItemChanged(current) // Refresh view to clear selection
                paletteAdapter.notifyItemChanged(current)
            }
        }
        btnSubmit.setOnClickListener {
            showSubmitConfirmationDialog()
        }

        // Setup ViewPager
        viewPager.isUserInputEnabled = false // Disable swipe to force buttons? Or keep enabled.
        // Usually tests allow swipe, but buttons are safer for state tracking.
        // Let's keep swipe enabled but sync index.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.jumpToQuestion(position)
            }
        })
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            viewPager.visibility = if (isLoading) android.view.View.GONE else android.view.View.VISIBLE
        }
        
        viewModel.test.observe(this) { test ->
            if (test != null) {
                toolbar.title = test.title
                // Show instructions bottom sheet before setting up test
                showTestInstructionsBottomSheet()
            }
        }

        viewModel.currentQuestionIndex.observe(this) { index ->
            if (viewPager.currentItem != index) {
                viewPager.setCurrentItem(index, true)
            }
            updateButtonStates(index)
            updateQuestionCounter(index)
        }

        viewModel.timeLeft.observe(this) { millis ->
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            val hours = (millis / (1000 * 60 * 60))
            tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        viewModel.submissionSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Test Submitted Successfully!", Toast.LENGTH_LONG).show()
                // Navigation handled by submissionResult observer
            }
        }
        
        viewModel.submissionResult.observe(this) { attempt ->
            if (attempt != null) {
                val intent = android.content.Intent(this, TestResultActivity::class.java)
                intent.putExtra("SCORE", attempt.score)
                intent.putExtra("CORRECT", attempt.correctCount)
                intent.putExtra("INCORRECT", attempt.incorrectCount)
                intent.putExtra("UNATTEMPTED", attempt.unattemptedCount)
                intent.putExtra("TIME_TAKEN", attempt.timeTaken)
                intent.putExtra("ATTEMPT_ID", attempt.id)
                startActivity(intent)
                finish()
            }
        }
        
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateQuestionCounter(index: Int) {
        val total = viewModel.getQuestions().size
        tvQuestionCounter.text = "${index + 1}/$total"
    }

    private fun setupAdapters() {
        val questions = viewModel.getQuestions()
        val test = viewModel.test.value ?: return
        
        // Create section title lookup
        val getSectionTitle: (Int) -> String = { position ->
            var count = 0
            var sectionTitle = ""
            for (section in test.sections) {
                if (position < count + section.questions.size) {
                    sectionTitle = section.title
                    break
                }
                count += section.questions.size
            }
            sectionTitle
        }
        
        questionAdapter = QuestionAdapter(
            questions,
            onAnswerChanged = { qId, answer ->
                viewModel.setAnswer(qId, answer)
                // Update palette - finding index is tricky with headers, but we can just notify all or find smart way
                // Ideally we should map question index to palette index, but notifyDataSetChanged is safe enough for now
                // or we can calculate the position.
                paletteAdapter.notifyDataSetChanged() 
            },
            getSavedAnswer = { qId -> viewModel.getAnswer(qId) },
            getSectionTitle = getSectionTitle
        )
        viewPager.adapter = questionAdapter
        
        // Add animation
        viewPager.setPageTransformer { page, position ->
            val minScale = 0.85f
            val minAlpha = 0.5f

            when {
                position < -1 -> { // [-Infinity,-1)
                    page.alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    val scaleFactor = Math.max(minScale, 1 - Math.abs(position))
                    val vertMargin = page.height * (1 - scaleFactor) / 2
                    val horzMargin = page.width * (1 - scaleFactor) / 2
                    if (position < 0) {
                        page.translationX = horzMargin - vertMargin / 2
                    } else {
                        page.translationX = -horzMargin + vertMargin / 2
                    }

                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor

                    page.alpha = minAlpha +
                            (scaleFactor - minScale) /
                            (1 - minScale) * (1 - minAlpha)
                }
                else -> { // (1,+Infinity]
                    page.alpha = 0f
                }
            }
        }
        
        // Initialize question counter
        updateQuestionCounter(viewModel.currentQuestionIndex.value ?: 0)

        // Prepare Palette Items with Sections
        val paletteItems = mutableListOf<PaletteAdapter.PaletteItem>()
        var globalIndex = 0
        test.sections.forEach { section ->
            paletteItems.add(PaletteAdapter.PaletteItem.Header(section.title))
            section.questions.forEach { question ->
                paletteItems.add(PaletteAdapter.PaletteItem.QuestionItem(question, globalIndex))
                globalIndex++
            }
        }

        paletteAdapter = PaletteAdapter(
            paletteItems,
            onQuestionClicked = { index ->
                viewModel.jumpToQuestion(index)
                drawerLayout.closeDrawer(GravityCompat.END)
            },
            getStatus = { qId ->
                val isAnswered = !viewModel.getAnswer(qId).isNullOrEmpty()
                val isReview = viewModel.isMarkedForReview(qId)
                
                when {
                    isAnswered && isReview -> PaletteAdapter.QuestionStatus.ANSWERED_AND_MARKED_FOR_REVIEW
                    isAnswered -> PaletteAdapter.QuestionStatus.ANSWERED
                    isReview -> PaletteAdapter.QuestionStatus.MARKED_FOR_REVIEW
                    else -> PaletteAdapter.QuestionStatus.NOT_ANSWERED
                }
            }
        )
        
        val layoutManager = GridLayoutManager(this, 5)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (paletteAdapter.getItemViewType(position)) {
                    PaletteAdapter.VIEW_TYPE_HEADER -> 5 // Span full width
                    else -> 1 // Span 1 column
                }
            }
        }
        
        rvPalette.layoutManager = layoutManager
        rvPalette.adapter = paletteAdapter
        
        // Handle Back Press
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }
    
    private fun showExitConfirmationDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Exit Test?")
            .setMessage("Are you sure you want to exit? Your progress will be lost if you haven't submitted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showSubmitConfirmationDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_submit_test)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val stats = viewModel.getTestStats()
        
        val tvAttempted = dialog.findViewById<TextView>(R.id.tvAttemptedCount)
        val tvSkipped = dialog.findViewById<TextView>(R.id.tvSkippedCount)
        val tvMarked = dialog.findViewById<TextView>(R.id.tvMarkedCount)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelSubmit)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmSubmit)
        
        tvAttempted.text = stats["attempted"].toString()
        tvSkipped.text = stats["skipped"].toString()
        tvMarked.text = stats["marked"].toString()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            viewModel.submitTest()
        }
        
        dialog.show()
    }
    
    private fun updateButtonStates(index: Int) {
        val total = viewModel.getQuestions().size
        btnPrevious.isEnabled = index > 0
        btnNext.isEnabled = index < total - 1
        if (index == total - 1) {
            btnNext.text = "Finish"
            btnNext.setOnClickListener { viewModel.submitTest() }
        } else {
            btnNext.text = "Next"
            btnNext.setOnClickListener { viewModel.nextQuestion() }
        }
    }
}
