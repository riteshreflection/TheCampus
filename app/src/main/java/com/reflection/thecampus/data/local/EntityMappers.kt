package com.reflection.thecampus.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reflection.thecampus.BasicInfo
import com.reflection.thecampus.Course
import com.reflection.thecampus.Pricing
import com.reflection.thecampus.Schedule
import com.reflection.thecampus.Test
import com.reflection.thecampus.UserProfile
import com.reflection.thecampus.data.local.entity.CourseEntity
import com.reflection.thecampus.data.local.entity.TestAttemptEntity
import com.reflection.thecampus.data.local.entity.TestEntity
import com.reflection.thecampus.data.local.entity.UserProfileEntity
import com.reflection.thecampus.data.model.TestAttempt
import com.reflection.thecampus.CourseContentItem

/**
 * Extension functions to convert between domain models and Room entities
 */

// ===== Course Mappers =====

fun Course.toEntity(isEnrolled: Boolean = false): CourseEntity {
    return CourseEntity(
        id = this.id,
        name = this.basicInfo.name,
        description = this.basicInfo.description,
        level = this.basicInfo.level,
        type = this.basicInfo.type,
        hasVideoExplanation = this.basicInfo.hasVideoExplanation,
        price = this.pricing.price,
        discount = this.pricing.discount,
        thumbnailUrl = this.pricing.thumbnailUrl,
        startDate = this.schedule.startDate,
        endDate = this.schedule.endDate,
        totalLectures = this.schedule.totalLectures,
        totalTests = this.schedule.totalTests,
        maxStudents = this.schedule.maxStudents,
        status = this.status,
        createdAt = this.createdAt,
        isEnrolled = isEnrolled,
        instructorIds = this.instructorIds,
        linkedTests = this.linkedTests,
        contentJson = gson.toJson(this.content)
    )
}

fun CourseEntity.toDomain(): Course {
    return Course(
        id = this.id,
        basicInfo = BasicInfo(
            name = this.name,
            description = this.description,
            level = this.level,
            type = this.type,
            hasVideoExplanation = this.hasVideoExplanation
        ),
        pricing = Pricing(
            price = this.price,
            discount = this.discount,
            thumbnailUrl = this.thumbnailUrl
        ),
        schedule = Schedule(
            startDate = this.startDate,
            endDate = this.endDate,
            totalLectures = this.totalLectures,
            totalTests = this.totalTests,
            maxStudents = this.maxStudents
        ),
        status = this.status,
        createdAt = this.createdAt,
        instructorIds = this.instructorIds,
        linkedTests = this.linkedTests,
        linkedClasses = emptyList(),
        announcements = emptyMap(),
        enrolledStudents = emptyMap(),
        content = try {
            val type = object : TypeToken<Map<String, CourseContentItem>>() {}.type
            gson.fromJson(this.contentJson, type)
        } catch (e: Exception) {
            emptyMap()
        }
    )
}

// ===== Test Mappers =====

fun com.reflection.thecampus.Test.toEntity(): TestEntity {
    return TestEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        subject = this.subject,
        duration = this.duration,
        totalMarks = this.totalMarks,
        totalQuestions = this.sections.values.sumOf { it.questions.size },
        status = this.status,
        isFree = this.isFree,
        level = this.level,
        createdAt = this.createdAt,
        createdBy = this.createdBy,
        courseId = this.courseId,
        explanationPdfUrl = this.explanationPdfUrl,
        explanationVideoUrl = this.explanationVideoUrl,
        maxAttempts = 0 // Not in original model
    )
}

fun TestEntity.toDomain(): Test {
    return Test(
        id = this.id,
        title = this.title,
        description = this.description,
        subject = this.subject,
        duration = this.duration,
        totalMarks = this.totalMarks,
        status = this.status,
        isFree = this.isFree,
        level = this.level,
        createdAt = this.createdAt,
        createdBy = this.createdBy,
        courseId = this.courseId,
        explanationPdfUrl = this.explanationPdfUrl,
        explanationVideoUrl = this.explanationVideoUrl,
        sections = emptyMap() // Questions stored separately
    )
}

// ===== UserProfile Mappers =====

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        userId = this.userId,
        email = this.email,
        fullName = this.fullName,
        mobileNumber = this.mobileNumber,
        profilePictureUrl = this.profilePictureUrl,
        createdAt = this.createdAt
    )
}

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        userId = this.userId,
        email = this.email,
        fullName = this.fullName,
        mobileNumber = this.mobileNumber,
        profilePictureUrl = this.profilePictureUrl,
        createdAt = this.createdAt
    )
}

// ===== TestAttempt Mappers =====

private val gson = Gson()

fun TestAttempt.toEntity(): TestAttemptEntity {
    val answersJson = gson.toJson(this.getAnswersMap())
    
    return TestAttemptEntity(
        id = this.id,
        testId = this.testId,
        testTitle = this.testTitle,
        studentId = this.studentId,
        submittedAt = this.submittedAt,
        timeTaken = this.timeTaken,
        score = this.score,
        correctCount = this.correctCount,
        incorrectCount = this.incorrectCount,
        unattemptedCount = this.unattemptedCount,
        answersJson = answersJson,
        isSynced = true // Already synced if we're storing from Firebase
    )
}

fun TestAttemptEntity.toDomain(): TestAttempt {
    val answersMap = try {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        gson.fromJson<Map<String, Any>>(this.answersJson, type)
    } catch (e: Exception) {
        emptyMap()
    }
    
    return TestAttempt(
        id = this.id,
        testId = this.testId,
        testTitle = this.testTitle,
        studentId = this.studentId,
        submittedAt = this.submittedAt,
        timeTaken = this.timeTaken,
        score = this.score,
        correctCount = this.correctCount,
        incorrectCount = this.incorrectCount,
        unattemptedCount = this.unattemptedCount,
        answers = answersMap,
        studentId_testId = "${this.studentId}_${this.testId}"
    )
}
