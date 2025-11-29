package com.reflection.thecampus

import android.content.Context
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.reflection.thecampus.data.local.AppDatabase
import com.reflection.thecampus.data.local.toDomain
import com.reflection.thecampus.data.local.toEntity
import com.reflection.thecampus.data.model.Offer
import com.reflection.thecampus.data.model.TaxSetting
import com.reflection.thecampus.utils.FirebaseConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class CourseRepository(context: Context) {

    private val database = FirebaseDatabase.getInstance()
    private val appDatabase = AppDatabase.getInstance(context)
    private val courseDao = appDatabase.courseDao()

    /**
     * Get all courses with cache-first strategy
     * Returns Flow that emits cached data immediately
     */
    fun getAllCoursesFlow(): Flow<List<Course>> = flow {
        val cachedFlow = courseDao.getAllCoursesFlow()
            .map { entities -> entities.map { it.toDomain() } }

        cachedFlow.collect { cachedCourses ->
            emit(cachedCourses)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get enrolled courses with cache-first strategy
     */
    fun getEnrolledCoursesFlow(userId: String): Flow<List<Course>> = flow {
        val cachedFlow = courseDao.getEnrolledCoursesFlow()
            .map { entities -> entities.map { it.toDomain() } }

        cachedFlow.collect { cachedCourses ->
            emit(cachedCourses)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get enrolled courses directly from Firebase Realtime Database
     * Listens for changes in users/$userId/enrolledCourses
     */
    fun getEnrolledCoursesRealtime(userId: String): Flow<List<Course>> = callbackFlow {
        Timber.d("getEnrolledCoursesRealtime() started for userId: $userId")

        val enrolledRef = database.getReference(FirebaseConstants.USERS)
            .child(userId)
            .child(FirebaseConstants.ENROLLED_COURSES)

        Timber.d("Firebase path: users/$userId/enrolledCourses")

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                Timber.d("Enrolled courses snapshot received - exists: ${snapshot.exists()}, childrenCount: ${snapshot.childrenCount}")

                val courseIds = snapshot.children.mapNotNull { it.key }
                Timber.d("Enrolled course IDs: ${courseIds.joinToString(", ")}")

                if (courseIds.isEmpty()) {
                    Timber.d("No enrolled courses found, emitting empty list")
                    trySend(emptyList())
                    return
                }

                launch {
                    val courses = mutableListOf<Course>()
                    var successCount = 0
                    var failCount = 0

                    courseIds.forEach { courseId ->
                        Timber.d("Fetching enrolled course: $courseId")
                        val course = getCourse(courseId)
                        if (course != null) {
                            courses.add(course)
                            successCount++
                            Timber.d("✓ Successfully fetched course $courseId: ${course.basicInfo.name}")
                        } else {
                            failCount++
                            Timber.w("⚠ Failed to fetch course $courseId")
                        }
                    }

                    Timber.d("Enrolled courses fetch complete: $successCount success, $failCount failed, total: ${courses.size}")
                    trySend(courses)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("❌ Enrolled courses listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        enrolledRef.addValueEventListener(listener)
        Timber.d("ValueEventListener attached to enrolled courses path")

        awaitClose {
            Timber.d("Removing enrolled courses listener")
            enrolledRef.removeEventListener(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get announcements for enrolled courses directly from Firebase
     */
    fun getAnnouncementsFlow(userId: String): Flow<List<AnnouncementItem>> = callbackFlow {
        val enrolledRef = database.getReference(FirebaseConstants.USERS)
            .child(userId)
            .child(FirebaseConstants.ENROLLED_COURSES)

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val courseIds = snapshot.children.mapNotNull { it.key }
                
                launch {
                    val announcements = mutableListOf<AnnouncementItem>()
                    
                    courseIds.forEach { courseId ->
                        try {
                            val courseSnapshot = database.getReference(FirebaseConstants.COURSES)
                                .child(courseId)
                                .get()
                                .await()
                            
                            val course = courseSnapshot.getValue(Course::class.java)
                            if (course != null) {
                                course.announcements.forEach { (id, announcement) ->
                                    announcements.add(
                                        AnnouncementItem(
                                            id = id,
                                            courseName = course.basicInfo.name,
                                            courseId = courseId,
                                            author = announcement.author,
                                            message = announcement.message,
                                            createdAt = announcement.createdAt,
                                            imageUrl = announcement.imageUrl
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error fetching course for announcements: $courseId")
                        }
                    }
                    
                    trySend(announcements.sortedByDescending { it.createdAt })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        enrolledRef.addValueEventListener(listener)
        awaitClose { enrolledRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Get all courses with real-time updates from Firebase
     */
    fun getAllCoursesRealtime(): Flow<List<Course>> = callbackFlow {
        val coursesRef = database.getReference(FirebaseConstants.COURSES)

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                launch {
                    val courses = mutableListOf<Course>()

                    for (courseSnapshot in snapshot.children) {
                        val course = courseSnapshot.getValue(Course::class.java)
                        if (course != null) {
                            val courseWithId = course.copy(id = courseSnapshot.key ?: "")
                            courses.add(courseWithId)
                        }
                    }

                    // Update cache
                    val entities = courses.map { it.toEntity(isEnrolled = false) }
                    courseDao.insertCourses(entities)

                    trySend(courses)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        coursesRef.addValueEventListener(listener)
        awaitClose { coursesRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Force refresh courses from Firebase (for pull-to-refresh)
     */
    suspend fun refreshCourses() = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference(FirebaseConstants.COURSES).get().await()
            val courses = mutableListOf<Course>()

            for (courseSnapshot in snapshot.children) {
                val course = courseSnapshot.getValue(Course::class.java)
                if (course != null) {
                    val courseWithId = course.copy(id = courseSnapshot.key ?: "")
                    courses.add(courseWithId)
                }
            }

            // Update Room cache
            val entities = courses.map { it.toEntity(isEnrolled = false) }
            courseDao.insertCourses(entities)

            Timber.d("Refreshed ${courses.size} courses from Firebase")
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing courses")
            throw e
        }
    }

    /**
     * Sync enrolled courses from Firebase
     */
    suspend fun syncEnrolledCourses(userId: String) = withContext(Dispatchers.IO) {
        try {
            Timber.d("syncEnrolledCourses() started for userId: $userId")

            // 1. Get enrolled course IDs
            val userCoursesSnapshot = database.getReference(FirebaseConstants.USERS)
                .child(userId)
                .child(FirebaseConstants.ENROLLED_COURSES)
                .get()
                .await()

            Timber.d("Enrolled courses snapshot - exists: ${userCoursesSnapshot.exists()}, childrenCount: ${userCoursesSnapshot.childrenCount}")

            val enrolledCourseIds = userCoursesSnapshot.children.mapNotNull { it.key }.toSet()
            Timber.d("Enrolled course IDs from Firebase: ${enrolledCourseIds.joinToString(", ")}")

            if (enrolledCourseIds.isEmpty()) {
                Timber.d("No enrolled courses for user $userId")
                return@withContext
            }

            // 2. Fetch all courses from Firebase
            Timber.d("Fetching all courses from Firebase...")
            val allCourses = fetchCoursesFromFirebase()
            Timber.d("Total courses fetched from Firebase: ${allCourses.size}")

            // 3. Filter enrolled courses
            val enrolledCourses = allCourses.filter { it.id in enrolledCourseIds }
            Timber.d("Filtered enrolled courses: ${enrolledCourses.size} out of ${allCourses.size}")
            enrolledCourses.forEach { course ->
                Timber.d("  - ${course.id}: ${course.basicInfo.name}")
            }

            // 4. Update Room cache with enrollment status
            val entities = enrolledCourses.map { it.toEntity(isEnrolled = true) }
            courseDao.insertCourses(entities)

            Timber.d("✓ Synced ${enrolledCourses.size} enrolled courses to cache")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error syncing enrolled courses: ${e.message}")
        }
    }

    /**
     * Get single course by ID (cache-first)
     */
    suspend fun getCourse(courseId: String): Course? = withContext(Dispatchers.IO) {
        try {
            Timber.d("getCourse() called with courseId: $courseId")

            // Bypass cache and fetch directly from Firebase
            Timber.d("Fetching course $courseId from Firebase...")

            val snapshot = database.getReference(FirebaseConstants.COURSES)
                .child(courseId)
                .get()
                .await()

            Timber.d("Firebase snapshot exists: ${snapshot.exists()}, hasChildren: ${snapshot.hasChildren()}")

            if (!snapshot.exists()) {
                Timber.w("⚠ Course $courseId does not exist in Firebase")
                return@withContext null
            }

            val course = snapshot.getValue(Course::class.java)
            Timber.d("Parsed course from Firebase: ${course?.basicInfo?.name ?: "null"}")

            val courseWithId = course?.copy(id = snapshot.key ?: courseId)

            // Update cache (fire and forget)
            if (courseWithId != null) {
                launch {
                    try {
                        courseDao.insertCourse(courseWithId.toEntity())
                        Timber.d("✓ Course $courseId cached in background")
                    } catch (e: Exception) {
                        Timber.e(e, "Error caching course")
                    }
                }
            } else {
                Timber.w("⚠ Failed to parse course $courseId from Firebase snapshot")
            }

            courseWithId
        } catch (e: Exception) {
            Timber.e(e, "❌ Error fetching course $courseId: ${e.message}")
            null
        }
    }

    /**
     * Legacy method for backward compatibility (uses cache)
     */
    suspend fun getCourses(): List<Course> = withContext(Dispatchers.IO) {
        try {
            // Get first emission from Flow (cached data)
            val cached = courseDao.getAllCoursesFlow()
                .map { entities -> entities.map { it.toDomain() } }
                .first()

            // If cache is empty, fetch from Firebase
            if (cached.isEmpty()) {
                val courses = fetchCoursesFromFirebase()
                val entities = courses.map { it.toEntity() }
                courseDao.insertCourses(entities)
                return@withContext courses
            }

            cached
        } catch (e: Exception) {
            Timber.e(e, "Error fetching courses")
            emptyList()
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    suspend fun getEnrolledCourses(userId: String): List<Course> = withContext(Dispatchers.IO) {
        try {
            // Get first emission from Flow (cached data)
            val cached = courseDao.getEnrolledCoursesFlow()
                .map { entities -> entities.map { it.toDomain() } }
                .first()

            // Sync in background (don't wait)
            CoroutineScope(Dispatchers.IO).launch {
                syncEnrolledCourses(userId)
            }

            cached
        } catch (e: Exception) {
            Timber.e(e, "Error fetching enrolled courses")
            emptyList()
        }
    }

    /**
     * Check enrollment status
     */
    suspend fun checkEnrollmentStatus(userId: String, courseId: String): Boolean {
        return try {
            val snapshot = database.getReference(FirebaseConstants.USERS)
                .child(userId)
                .child(FirebaseConstants.ENROLLED_COURSES)
                .child(courseId)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            Timber.e(e, "Error checking enrollment status")
            false
        }
    }

    /**
     * Enroll in course
     */
    suspend fun enrollInCourse(userId: String, courseId: String): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "/${FirebaseConstants.USERS}/$userId/${FirebaseConstants.ENROLLED_COURSES}/$courseId" to true,
                "/${FirebaseConstants.COURSES}/$courseId/${FirebaseConstants.ENROLLED_STUDENTS}/$userId" to true
            )
            database.reference.updateChildren(updates).await()

            // Update local cache
            courseDao.markAsEnrolled(courseId)

            true
        } catch (e: Exception) {
            Timber.e(e, "Error enrolling in course")
            false
        }
    }

    /**
     * Get offers (not cached yet - Phase 3)
     */
    suspend fun getOffers(): List<Offer> {
        return try {
            val snapshot = database.getReference(FirebaseConstants.OFFERS).get().await()
            val offers = mutableListOf<Offer>()
            for (offerSnapshot in snapshot.children) {
                val offer = offerSnapshot.getValue(Offer::class.java)
                if (offer != null) {
                    offers.add(offer)
                }
            }
            offers
        } catch (e: Exception) {
            Timber.e(e, "Error fetching offers")
            emptyList()
        }
    }

    /**
     * Get tax settings (not cached yet - Phase 3)
     */
    suspend fun getTaxSettings(): List<TaxSetting> {
        return try {
            val snapshot = database.getReference(FirebaseConstants.SITE_SETTINGS)
                .child(FirebaseConstants.TAXES)
                .get()
                .await()
            val taxes = mutableListOf<TaxSetting>()
            for (taxSnapshot in snapshot.children) {
                val tax = taxSnapshot.getValue(TaxSetting::class.java)
                if (tax != null) {
                    taxes.add(tax)
                }
            }
            taxes
        } catch (e: Exception) {
            Timber.e(e, "Error fetching tax settings")
            emptyList()
        }
    }

    /**
     * Clear all cached courses (for logout)
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            courseDao.deleteAll()
            Timber.d("Course cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing cache")
        }
    }

    // ===== Private Helper Methods =====

    private suspend fun fetchCoursesFromFirebase(): List<Course> {
        val snapshot = database.getReference(FirebaseConstants.COURSES).get().await()
        val courses = mutableListOf<Course>()

        for (courseSnapshot in snapshot.children) {
            val course = courseSnapshot.getValue(Course::class.java)
            if (course != null) {
                val courseWithId = course.copy(id = courseSnapshot.key ?: "")
                courses.add(courseWithId)
            }
        }

        return courses
    }
}