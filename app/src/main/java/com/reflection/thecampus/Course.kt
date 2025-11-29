package com.reflection.thecampus

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@IgnoreExtraProperties
data class Course(
    val id: String = "",
    val basicInfo: BasicInfo = BasicInfo(),
    val pricing: Pricing = Pricing(),
    val schedule: Schedule = Schedule(),
    val instructorIds: List<String> = emptyList(),
    val linkedTests: List<String> = emptyList(),
    val linkedClasses: List<String> = emptyList(),
    val status: String = "draft",
    val createdAt: Long = 0,
    val announcements: Map<String, Announcement> = emptyMap(),
    val enrolledStudents: Map<String, Boolean> = emptyMap(),
    val content: Map<String, CourseContentItem> = emptyMap()
) : Parcelable

@Keep
@Parcelize
@IgnoreExtraProperties
data class BasicInfo(
    val name: String = "",
    val description: String = "",
    val level: String = "",
    val type: String = "",
    val hasVideoExplanation: Boolean = false
) : Parcelable

@Keep
@Parcelize
@IgnoreExtraProperties
data class Pricing(
    val price: Double = 0.0,
    val discount: Int = 0,
    val thumbnailUrl: String = ""
) : Parcelable

@Keep
@Parcelize
@IgnoreExtraProperties
data class Schedule(
    val startDate: String = "",
    val endDate: String = "",
    val totalLectures: Int = 0,
    val totalTests: Int = 0,
    val maxStudents: Int = 0
) : Parcelable

@Keep
@Parcelize
@IgnoreExtraProperties
data class Announcement(
    val author: String = "",
    val message: String = "",
    val createdAt: Long = 0,
    val imageUrl: String = ""
) : Parcelable

@Keep
@Parcelize
@IgnoreExtraProperties
data class CourseContentItem(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "folder" or "file"
    val status: String = "published",
    val isPublic: Boolean = false,
    val parentId: String? = null,
    val url: String = ""
) : Parcelable

