package com.reflection.thecampus.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Test(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val duration: Int = 0, // in minutes
    val totalMarks: Int = 0,
    val totalQuestions: Int = 0,
    val status: String = "",
    @get:PropertyName("isFree") @set:PropertyName("isFree") var isFree: Boolean = false,
    val level: String = "",
    val createdAt: Long = 0,
    val createdBy: String = "",
    val courseId: String = "",
    val explanationPdfUrl: String = "",
    val explanationVideoUrl: String = "",
    val maxAttempts: Int = 0,
    // We will handle sections manually because of schema inconsistency (List vs Map)
    @PropertyName("sections")
    val _sections: @kotlinx.parcelize.RawValue Any? = null
) : Parcelable {
    
    val sections: List<Section>
        get() = when (_sections) {
            is List<*> -> {
                _sections.mapNotNull { 
                    if (it is Map<*, *>) {
                        // We need to convert the Map to Section object manually or use a mapper
                        // But wait, if it's a List of objects, Firebase mapper converts them to HashMaps usually if not typed?
                        // No, getValue(Test::class.java) converts nested objects if typed.
                        // But here _sections is Any, so Firebase converts to List<HashMap> or HashMap<String, HashMap>.
                        // We need to manually map HashMap to Section.
                        // This is getting complicated.
                        // Simpler approach: Use ObjectMapper or manual mapping.
                        // Since we are using Any, we get Maps.
                        mapToSection(it as Map<String, Any>)
                    } else null
                }
            }
            is Map<*, *> -> {
                _sections.values.mapNotNull { 
                    if (it is Map<*, *>) mapToSection(it as Map<String, Any>) else null
                }.sortedBy { it.id }
            }
            else -> emptyList()
        }

    // Helper to map Map to Section
    private fun mapToSection(map: Map<String, Any>): Section {
        return Section(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            defaultMarks = (map["defaultMarks"] as? Number)?.toDouble() ?: 0.0,
            defaultNegativeMarks = (map["defaultNegativeMarks"] as? Number)?.toDouble() ?: 0.0,
            _questions = map["questions"]
        )
    }

    // Helper methods
    fun getTotalQuestionCount(): Int = sections.sumOf { it.questions.size }
    
    fun getAllQuestions(): List<Question> {
        val allQuestions = mutableListOf<Question>()
        sections.forEach { section ->
            allQuestions.addAll(section.questions)
        }
        return allQuestions
    }
    
    fun isValid(): Boolean {
        return id.isNotEmpty() && 
               title.isNotEmpty() && 
               duration > 0 && 
               sections.isNotEmpty()
    }
}

@Keep
@Parcelize
data class Section(
    val id: String = "",
    val title: String = "",
    val defaultMarks: Double = 0.0,
    val defaultNegativeMarks: Double = 0.0,
    @PropertyName("questions")
    val _questions: @kotlinx.parcelize.RawValue Any? = null
) : Parcelable {
    
    val questions: List<Question>
        get() = when (_questions) {
            is List<*> -> {
                _questions.mapNotNull { 
                    if (it is Map<*, *>) mapToQuestion(it as Map<String, Any>) else null
                }
            }
            is Map<*, *> -> {
                _questions.values.mapNotNull { 
                    if (it is Map<*, *>) mapToQuestion(it as Map<String, Any>) else null
                }.sortedBy { it.id }
            }
            else -> emptyList()
        }

    private fun mapToQuestion(map: Map<String, Any>): Question {
        return Question(
            id = map["id"] as? String ?: "",
            questionText = map["questionText"] as? String ?: "",
            questionImage = map["questionImage"] as? String ?: "",
            questionType = map["questionType"] as? String ?: "",
            options = (map["options"] as? Map<String, String>) ?: emptyMap(),
            correctAnswers = (map["correctAnswers"] as? List<String>) ?: emptyList(),
            correctNumericalAnswerRange = map["correctNumericalAnswerRange"]?.let { 
                val rangeMap = it as? Map<String, Any>
                if (rangeMap != null) {
                    NumericalRange(
                        from = (rangeMap["from"] as? Number)?.toDouble() ?: 0.0,
                        to = (rangeMap["to"] as? Number)?.toDouble() ?: 0.0
                    )
                } else null
            },
            marks = (map["marks"] as? Number)?.toDouble() ?: 0.0,
            negativeMarks = (map["negativeMarks"] as? Number)?.toDouble() ?: 0.0,
            explanation = map["explanation"]?.let {
                val expMap = it as? Map<String, Any>
                if (expMap != null) {
                    Explanation(
                        text = expMap["text"] as? String ?: "",
                        imageUrl = expMap["imageUrl"] as? String ?: ""
                    )
                } else null
            },
            imageFileName = map["imageFileName"] as? String ?: ""
        )
    }
}

@Keep
@Parcelize
data class Question(
    val id: String = "",
    val questionText: String = "",
    val questionImage: String = "",
    val questionType: String = "", // MCQ, MSQ, ShortAnswer
    val options: Map<String, String> = emptyMap(),
    val correctAnswers: List<String> = emptyList(),
    val correctNumericalAnswerRange: NumericalRange? = null,
    val marks: Double = 0.0,
    val negativeMarks: Double = 0.0,
    val explanation: Explanation? = null,
    val imageFileName: String = ""
) : Parcelable {
    
    // Helper methods for question type checking
    fun isMCQ(): Boolean = questionType.equals("MCQ", ignoreCase = true)
    fun isMSQ(): Boolean = questionType.equals("MSQ", ignoreCase = true)
    fun isShortAnswer(): Boolean = questionType.equals("ShortAnswer", ignoreCase = true) || 
                                    questionType.equals("NAT", ignoreCase = true)
    
    fun hasImage(): Boolean = questionImage.isNotEmpty()
    
    fun hasExplanation(): Boolean = explanation != null && 
                                    (explanation.text.isNotEmpty() || explanation.imageUrl.isNotEmpty())
    
    // Validate user answer
    fun validateAnswer(userAnswer: String?): Boolean {
        if (userAnswer.isNullOrEmpty()) return false
        
        return when {
            isShortAnswer() -> validateNumericalAnswer(userAnswer)
            isMSQ() -> validateMSQAnswer(userAnswer)
            isMCQ() -> correctAnswers.contains(userAnswer)
            else -> false
        }
    }
    
    private fun validateNumericalAnswer(answer: String): Boolean {
        val userValue = answer.toDoubleOrNull() ?: return false
        val range = correctNumericalAnswerRange ?: return false
        return userValue in range.from..range.to
    }
    
    private fun validateMSQAnswer(answer: String): Boolean {
        val userAnswers = answer.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val correctSet = correctAnswers.toSet()
        return userAnswers == correctSet
    }
}

@Keep
@Parcelize
data class NumericalRange(
    val from: Double = 0.0,
    val to: Double = 0.0
) : Parcelable

@Keep
@Parcelize
data class Explanation(
    val text: String = "",
    val imageUrl: String = ""
) : Parcelable

@Keep
@Parcelize
data class TestAttempt(
    val id: String = "",
    val testId: String = "",
    val testTitle: String = "", // Added for easier display in history
    val studentId: String = "",
    val submittedAt: Long = 0,
    val timeTaken: Long = 0,
    val score: Double = 0.0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unattemptedCount: Int = 0,
    // We use Any to support List<String> (MCQ/MSQ) and Number (ShortAnswer)
    // Changing to Any? to handle Firebase's array optimization (if keys are integers)
    val answers: @kotlinx.parcelize.RawValue Any? = null,
    val studentId_testId: String = ""
) : Parcelable {
    
    fun getAnswersMap(): Map<String, Any> {
        return when (answers) {
            is Map<*, *> -> answers as Map<String, Any>
            is List<*> -> {
                // Convert List to Map with string indices
                val map = mutableMapOf<String, Any>()
                answers.forEachIndexed { index, value ->
                    if (value != null) {
                        map[index.toString()] = value
                    }
                }
                map
            }
            else -> emptyMap()
        }
    }
}
