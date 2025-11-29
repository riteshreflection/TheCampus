package com.reflection.thecampus

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
    val status: String = "",
    @get:PropertyName("isFree") @set:PropertyName("isFree") var isFree: Boolean = false,
    val level: String = "",
    val createdAt: Long = 0,
    val createdBy: String = "",
    val courseId: String = "",
    val explanationPdfUrl: String = "",
    val explanationVideoUrl: String = "",
    // Sections map
    val sections: Map<String, Section> = emptyMap()
) : Parcelable

@Keep
@Parcelize
data class TestSummary(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val duration: Int = 0, // in minutes
    val totalMarks: Int = 0,
    val status: String = "",
    @get:PropertyName("isFree") @set:PropertyName("isFree") var isFree: Boolean = false,
    val level: String = "",
    val createdAt: Long = 0,
    val createdBy: String = "",
    val courseId: String = "",
    val explanationPdfUrl: String = "",
    val explanationVideoUrl: String = ""
) : Parcelable

@Keep
@Parcelize
data class Section(
    val id: String = "",
    val title: String = "",
    val questions: ArrayList<Question> = ArrayList()
) : Parcelable

@Keep
@Parcelize
data class Question(
    val id: String = "",
    val questionText: String = "",
    val questionImage: String = "",
    val questionType: String = "", // MCQ, MSQ, ShortAnswer
    val options: Map<String, String> = emptyMap(), // Keep options as Map for "a", "b" keys
    val correctAnswers: List<String> = emptyList(),
    val correctNumericalAnswerRange: NumericalRange? = null,
    val marks: Int = 0,
    val negativeMarks: Double = 0.0, // Changed to Double as negative marks can be fractional
    val explanation: Explanation? = null
) : Parcelable

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
