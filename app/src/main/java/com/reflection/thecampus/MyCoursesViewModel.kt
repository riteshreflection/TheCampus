package com.reflection.thecampus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyCoursesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application.applicationContext)
    private val _enrolledCourses = MutableLiveData<List<Course>>()
    val enrolledCourses: LiveData<List<Course>> = _enrolledCourses

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        startEnrolledCoursesObservation()
    }

    private fun startEnrolledCoursesObservation() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        timber.log.Timber.d("MyCoursesViewModel: startEnrolledCoursesObservation()")
        timber.log.Timber.d("Current user: ${currentUser?.uid ?: "null (not authenticated)"}")

        if (currentUser != null) {
            timber.log.Timber.d("Starting Flow collection for enrolled courses...")
            viewModelScope.launch {
                repository.getEnrolledCoursesRealtime(currentUser.uid)
                    .collect { courses ->
                        timber.log.Timber.d("📥 Enrolled courses Flow emitted: ${courses.size} courses")
                        courses.forEachIndexed { index, course ->
                            timber.log.Timber.d("  ${index + 1}. ${course.id}: ${course.basicInfo.name}")
                        }

                        _enrolledCourses.value = courses
                        _isRefreshing.value = false

                        if (courses.isEmpty()) {
                            timber.log.Timber.d("⚠ No enrolled courses found for user")
                        } else {
                            timber.log.Timber.d("✓ Enrolled courses LiveData updated with ${courses.size} courses")
                        }
                    }
            }
        } else {
            timber.log.Timber.w("⚠ No authenticated user, setting empty enrolled courses list")
            _enrolledCourses.value = emptyList()
            _isRefreshing.value = false
        }
    }
    
    fun refresh() {
        _isRefreshing.value = true
        // The real-time listener will automatically update with latest data
        // Just trigger a manual sync in background
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    repository.syncEnrolledCourses(currentUser.uid)
                } catch (e: Exception) {
                    _error.value = e.message
                } finally {
                    _isRefreshing.value = false
                }
            }
        } else {
            _isRefreshing.value = false
        }
    }
}
