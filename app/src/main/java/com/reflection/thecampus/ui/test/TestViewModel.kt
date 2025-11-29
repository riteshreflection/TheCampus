package com.reflection.thecampus.ui.test

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reflection.thecampus.data.model.Question
import com.reflection.thecampus.data.model.Test
import com.reflection.thecampus.data.model.TestAttempt
import com.reflection.thecampus.data.repository.TestRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class TestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TestRepository(application.applicationContext)

    private val _test = MutableLiveData<Test?>()
    val test: LiveData<Test?> = _test

    private val _currentQuestionIndex = MutableLiveData(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _timeLeft = MutableLiveData<Long>()
    val timeLeft: LiveData<Long> = _timeLeft
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submissionSuccess = MutableLiveData<Boolean>()
    val submissionSuccess: LiveData<Boolean> = _submissionSuccess
    
    private val _submissionResult = MutableLiveData<TestAttempt?>()
    val submissionResult: LiveData<TestAttempt?> = _submissionResult
    
    private val _submissionMessage = MutableLiveData<String?>()
    val submissionMessage: LiveData<String?> = _submissionMessage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _unsyncedCount = MutableLiveData<Int>(0)
    val unsyncedCount: LiveData<Int> = _unsyncedCount

    // Map<QuestionId, Answer>
    val answers = mutableMapOf<String, String>()
    // Map<QuestionId, IsMarkedForReview>
    val reviewStatus = mutableMapOf<String, Boolean>()

    private var timer: CountDownTimer? = null
    private var questionsList = listOf<Question>()
    private var startTime: Long = 0
    private var lastSavedTime: Long = 0

    init {
        checkUnsyncedAttempts()
    }

    fun loadTest(testId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val testData = repository.getTest(testId)
                setupQuestions(testData)  // Setup questions FIRST
                _test.value = testData    // Then set test value (triggers observer)
                startTimer(testData.duration * 60 * 1000L)
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load test: ${e.message}"
                Timber.e(e, "Error loading test")
            }
        }
    }

    private fun setupQuestions(test: Test) {
        val list = mutableListOf<Question>()
        test.sections.forEach { section ->
            list.addAll(section.questions)
        }
        questionsList = list
        Timber.d("setupQuestions: Total questions = ${questionsList.size}")
    }

    fun getQuestions(): List<Question> = questionsList

    private fun startTimer(durationMillis: Long) {
        startTime = System.currentTimeMillis()
        lastSavedTime = startTime
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished
                
                // Auto-save answers every 30 seconds
                if (System.currentTimeMillis() - lastSavedTime > 30000) {
                    autoSaveAnswers()
                    lastSavedTime = System.currentTimeMillis()
                }
            }

            override fun onFinish() {
                submitTest()
            }
        }.start()
    }
    
    /**
     * Auto-save answers locally (for crash recovery)
     */
    private fun autoSaveAnswers() {
        // TODO: Implement local answer caching if needed
        Timber.d("Auto-saving answers...")
    }

    fun setAnswer(questionId: String, answer: String) {
        answers[questionId] = answer
    }

    fun clearAnswer(questionId: String) {
        answers.remove(questionId)
    }

    fun toggleMarkForReview(questionId: String) {
        val current = reviewStatus[questionId] ?: false
        reviewStatus[questionId] = !current
    }

    fun isMarkedForReview(questionId: String): Boolean {
        return reviewStatus[questionId] ?: false
    }

    fun getAnswer(questionId: String): String? {
        return answers[questionId]
    }

    fun nextQuestion() {
        val current = _currentQuestionIndex.value ?: 0
        if (current < questionsList.size - 1) {
            _currentQuestionIndex.value = current + 1
        }
    }

    fun previousQuestion() {
        val current = _currentQuestionIndex.value ?: 0
        if (current > 0) {
            _currentQuestionIndex.value = current - 1
        }
    }

    fun jumpToQuestion(index: Int) {
        if (index in questionsList.indices) {
            _currentQuestionIndex.value = index
        }
    }

    fun submitTest() {
        if (_isSubmitting.value == true) return
        _isSubmitting.value = true
        timer?.cancel()

        val testData = _test.value ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var score = 0.0
        var correctCount = 0
        var incorrectCount = 0
        var unattemptedCount = 0
        
        val finalAnswers = mutableMapOf<String, Any>()

        questionsList.forEach { question ->
            val userAnswerStr = answers[question.id]
            
            if (userAnswerStr.isNullOrEmpty()) {
                unattemptedCount++
            } else {
                var isCorrect = false
                
                when (question.questionType) {
                    "MCQ" -> {
                        // Store as String e.g. "a"
                        finalAnswers[question.id] = userAnswerStr
                        isCorrect = question.correctAnswers.contains(userAnswerStr)
                    }
                    "MSQ" -> {
                        // Store as List<String> e.g. ["a", "c"]
                        val userList = userAnswerStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.sorted()
                        finalAnswers[question.id] = userList
                        
                        val correctList = question.correctAnswers.sorted()
                        isCorrect = userList == correctList
                    }
                    "ShortAnswer", "NAT" -> {
                        // Store as Number e.g. 50 or 50.5
                        val userVal = userAnswerStr.toDoubleOrNull()
                        if (userVal != null) {
                            // Check if it's an integer
                            if (userVal % 1.0 == 0.0) {
                                finalAnswers[question.id] = userVal.toInt()
                            } else {
                                finalAnswers[question.id] = userVal
                            }
                            
                            val range = question.correctNumericalAnswerRange
                            if (range != null) {
                                isCorrect = userVal >= range.from && userVal <= range.to
                            }
                        } else {
                            // Invalid number format, treat as incorrect
                            finalAnswers[question.id] = userAnswerStr // Fallback
                            isCorrect = false
                        }
                    }
                    else -> {
                        // Default fallback
                        finalAnswers[question.id] = userAnswerStr
                        isCorrect = false
                    }
                }

                if (isCorrect) {
                    score += question.marks
                    correctCount++
                } else {
                    score -= question.negativeMarks
                    incorrectCount++
                }
            }
        }

        // FIX: Calculate time taken in SECONDS (not milliseconds)
        val timeTakenMillis = System.currentTimeMillis() - startTime
        val timeTakenSeconds = timeTakenMillis / 1000 // Convert to seconds
        
        val attempt = TestAttempt(
            testId = testData.id,
            testTitle = testData.title,
            studentId = userId,
            submittedAt = System.currentTimeMillis(),
            timeTaken = timeTakenSeconds, // Store in SECONDS
            score = score,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unattemptedCount = unattemptedCount,
            answers = finalAnswers,
            studentId_testId = "${userId}_${testData.id}"
        )

        // Submit with offline support
        viewModelScope.launch {
            val (success, attemptWithId, message) = repository.submitTestAttempt(attempt)
            
            _submissionSuccess.value = success
            _submissionMessage.value = message
            _isSubmitting.value = false
            
            if (success) {
                _submissionResult.value = attemptWithId
            } else {
                _error.value = message
                // Update unsynced count
                checkUnsyncedAttempts()
            }
        }
    }
    
    /**
     * Retry failed submissions
     */
    fun retrySubmissions() {
        viewModelScope.launch {
            val successCount = repository.retryFailedSubmissions()
            if (successCount > 0) {
                _submissionMessage.value = "Successfully synced $successCount test(s)"
                checkUnsyncedAttempts()
            } else {
                _submissionMessage.value = "No pending submissions to sync"
            }
        }
    }
    
    /**
     * Check for unsynced attempts
     */
    private fun checkUnsyncedAttempts() {
        viewModelScope.launch {
            val count = repository.getUnsyncedCount()
            _unsyncedCount.value = count
            Timber.d("Unsynced attempts: $count")
        }
    }

    private fun checkAnswer(question: Question, userAnswer: String): Boolean {
        return question.validateAnswer(userAnswer)
    }

    fun getTestStats(): Map<String, Int> {
        var attempted = 0
        var marked = 0
        var skipped = 0
        
        questionsList.forEach { question ->
            val isAnswered = !answers[question.id].isNullOrEmpty()
            val isMarked = reviewStatus[question.id] == true
            
            if (isAnswered) attempted++
            else skipped++
            
            if (isMarked) marked++
        }
        
        return mapOf(
            "attempted" to attempted,
            "skipped" to skipped,
            "marked" to marked,
            "total" to questionsList.size
        )
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
