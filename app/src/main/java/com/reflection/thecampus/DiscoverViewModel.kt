package com.reflection.thecampus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application.applicationContext)
    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _enrolledCourseIds = MutableLiveData<Set<String>>()
    val enrolledCourseIds: LiveData<Set<String>> = _enrolledCourseIds

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        startCoursesObservation()
        startEnrolledCoursesObservation()
    }

    private fun startCoursesObservation() {
        timber.log.Timber.d("DiscoverViewModel: startCoursesObservation()")
        viewModelScope.launch {
            timber.log.Timber.d("Starting Flow collection for all courses...")
            repository.getAllCoursesRealtime()
                .collect { courseList ->
                    timber.log.Timber.d("📥 All courses Flow emitted: ${courseList.size} courses")
                    courseList.forEachIndexed { index, course ->
                        timber.log.Timber.d("  ${index + 1}. ${course.id}: ${course.basicInfo.name}")
                    }

                    _courses.value = courseList
                    _isRefreshing.value = false

                    if (courseList.isEmpty()) {
                        timber.log.Timber.w("⚠ No courses found in Firebase")
                    } else {
                        timber.log.Timber.d("✓ Courses LiveData updated with ${courseList.size} courses")
                    }
                }
        }
    }

    private fun startEnrolledCoursesObservation() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        timber.log.Timber.d("DiscoverViewModel: startEnrolledCoursesObservation()")
        timber.log.Timber.d("Current user: ${currentUser?.uid ?: "null (not authenticated)"}")

        if (currentUser != null) {
            viewModelScope.launch {
                timber.log.Timber.d("Starting Flow collection for enrolled course IDs...")
                repository.getEnrolledCoursesRealtime(currentUser.uid)
                    .collect { enrolledCourses ->
                        val enrolledIds = enrolledCourses.map { it.id }.toSet()
                        timber.log.Timber.d("📥 Enrolled course IDs updated: ${enrolledIds.size} courses")
                        timber.log.Timber.d("  IDs: ${enrolledIds.joinToString(", ")}")

                        _enrolledCourseIds.value = enrolledIds
                    }
            }
        } else {
            timber.log.Timber.d("No authenticated user, setting empty enrolled course IDs")
            _enrolledCourseIds.value = emptySet()
        }
    }
    
    fun refresh() {
        timber.log.Timber.d("DiscoverViewModel: refresh() called")
        _isRefreshing.value = true
        // The real-time listener will automatically update with latest data
        // Just trigger a manual refresh in background
        viewModelScope.launch {
            try {
                timber.log.Timber.d("Calling repository.refreshCourses()...")
                repository.refreshCourses()
                timber.log.Timber.d("✓ Refresh completed successfully")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "❌ Error during refresh: ${e.message}")
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
