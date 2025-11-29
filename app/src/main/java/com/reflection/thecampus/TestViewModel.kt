package com.reflection.thecampus

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TestViewModel : ViewModel() {

    private val _test = MutableLiveData<Test?>()
    val test: LiveData<Test?> = _test

    private val _currentQuestionIndex = MutableLiveData(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _timeLeft = MutableLiveData<Long>()
    val timeLeft: LiveData<Long> = _timeLeft

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submissionSuccess = MutableLiveData<Boolean>()
    val submissionSuccess: LiveData<Boolean> = _submissionSuccess

    // Map<QuestionId, Answer>
    val answers = mutableMapOf<String, String>()
    // Map<QuestionId, IsMarkedForReview>
    val reviewStatus = mutableMapOf<String, Boolean>()

    private var timer: CountDownTimer? = null
    private var questionsList = listOf<Question>()
    private var startTime: Long = 0

    fun loadTest(testId: String) {
        val database = FirebaseDatabase.getInstance()
        database.getReference("tests").child(testId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val testData = snapshot.getValue(Test::class.java)
                    if (testData != null) {
                        _test.value = testData
                        setupQuestions(testData)
                        startTimer(testData.duration * 60 * 1000L)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun setupQuestions(test: Test) {
        val list = mutableListOf<Question>()
        // Flatten sections if they exist
        if (test.sections.isNotEmpty()) {
            test.sections.values.forEach { section ->
                list.addAll(section.questions)
            }
        } 
        
        questionsList = list
    }

    fun getQuestions(): List<Question> = questionsList

    private fun startTimer(durationMillis: Long) {
        startTime = System.currentTimeMillis()
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                submitTest()
            }
        }.start()
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

        questionsList.forEach { question ->
            val userAnswer = answers[question.id]
            if (userAnswer.isNullOrEmpty()) {
                unattemptedCount++
            } else {
                val isCorrect = checkAnswer(question, userAnswer)
                if (isCorrect) {
                    score += question.marks
                    correctCount++
                } else {
                    score -= question.negativeMarks
                    incorrectCount++
                }
            }
        }

        val timeTaken = System.currentTimeMillis() - startTime
        
        val attempt = mapOf(
            "testId" to testData.id,
            "studentId" to userId,
            "submittedAt" to System.currentTimeMillis(),
            "timeTaken" to timeTaken,
            "score" to score,
            "correctCount" to correctCount,
            "incorrectCount" to incorrectCount,
            "unattemptedCount" to unattemptedCount,
            "answers" to answers
        )

        val database = FirebaseDatabase.getInstance()
        val attemptRef = database.getReference("testAttempts").push()
        attemptRef.setValue(attempt)
            .addOnSuccessListener {
                _submissionSuccess.value = true
                _isSubmitting.value = false
            }
            .addOnFailureListener {
                _isSubmitting.value = false
                // Handle failure
            }
    }

    private fun checkAnswer(question: Question, userAnswer: String): Boolean {
        // Simple check for MCQ/ShortAnswer
        // For MCQ, correctAnswers is a list. Check if userAnswer is in it.
        // For ShortAnswer, check range or exact match.
        
        if (question.questionType == "ShortAnswer") {
            val userVal = userAnswer.toDoubleOrNull()
            if (userVal != null && question.correctNumericalAnswerRange != null) {
                return userVal >= question.correctNumericalAnswerRange.from && userVal <= question.correctNumericalAnswerRange.to
            }
            // Fallback to exact string match if range not provided (though model has range)
            return false
        } else {
            // MCQ / MSQ
            // For MCQ, userAnswer is usually a single option key like "a"
            // correctAnswers is List<String> like ["a"]
            return question.correctAnswers.contains(userAnswer)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
