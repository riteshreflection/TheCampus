package com.reflection.thecampus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for MainActivity to centralize data fetching
 * and share data across fragments
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    private val _allCourses = MutableLiveData<List<Course>>()
    val allCourses: LiveData<List<Course>> = _allCourses

    private val _enrolledCourses = MutableLiveData<List<Course>>()
    val enrolledCourses: LiveData<List<Course>> = _enrolledCourses

    private val _discoverCourses = MutableLiveData<List<Course>>()
    val discoverCourses: LiveData<List<Course>> = _discoverCourses

    private val _announcements = MutableLiveData<List<AnnouncementItem>>()
    val announcements: LiveData<List<AnnouncementItem>> = _announcements

    private val _appStatus = MutableLiveData<AppStatus>()
    val appStatus: LiveData<AppStatus> = _appStatus
    
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        loadData()
        checkAppStatus()
    }

    private fun loadData() {
        val currentUser = auth.currentUser

        viewModelScope.launch {
            if (currentUser != null) {
                // Load enrolled courses from cache
                val courses = repository.getEnrolledCourses(currentUser.uid)
                _enrolledCourses.value = courses
                
                // NEW: Fetch announcements directly from Firebase (Realtime)
                launch {
                    repository.getAnnouncementsFlow(currentUser.uid).collect { items ->
                        _announcements.value = items
                    }
                }
                
                // Sync in background
                repository.syncEnrolledCourses(currentUser.uid)
            } else {
                _enrolledCourses.value = emptyList()
                _announcements.value = emptyList()
            }

            // Load all courses for discovery
            val allCoursesList = repository.getCourses()
            _allCourses.value = allCoursesList
            // Filter out enrolled courses for discovery
            val enrolledIds = _enrolledCourses.value?.map { it.id } ?: emptyList()
            _discoverCourses.value = allCoursesList.filter { it.id !in enrolledIds }
        }
    }
    
    /**
     * Force refresh data (for pull-to-refresh)
     */
    fun refreshData() {
        _isRefreshing.value = true
        val currentUser = auth.currentUser
        
        viewModelScope.launch {
            try {
                // Refresh courses from Firebase
                repository.refreshCourses()
                
                if (currentUser != null) {
                    // Sync enrolled courses
                    repository.syncEnrolledCourses(currentUser.uid)
                    
                    // Reload from cache
                    val courses = repository.getEnrolledCourses(currentUser.uid)
                    _enrolledCourses.value = courses
                    
                    // Announcements are auto-updated via Flow, no need to extract
                }
                
                // Reload all courses
                val allCoursesList = repository.getCourses()
                _allCourses.value = allCoursesList
                val enrolledIds = _enrolledCourses.value?.map { it.id } ?: emptyList()
                _discoverCourses.value = allCoursesList.filter { it.id !in enrolledIds }
                
                checkAppStatus()
            } finally {
                _isRefreshing.value = false
            }
        }
    }



    private fun checkAppStatus() {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val currentUser = auth.currentUser

        // 1. Check Site Settings (Maintenance & Update)
        database.getReference("siteSettings").child("appControls").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val isAndroidActive = snapshot.child("isAndroidActive").getValue(Boolean::class.java) ?: true
                val serverVersion = snapshot.child("androidVersion").getValue(String::class.java) ?: "1.0"
                
                if (!isAndroidActive) {
                    _appStatus.postValue(AppStatus.Maintenance)
                    return
                }

                // Check Version
                val currentVersion = try {
                    val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                    pInfo.versionName
                } catch (e: Exception) {
                    "1.0"
                }

                if (isUpdateRequired(currentVersion, serverVersion)) {
                    _appStatus.postValue(AppStatus.ForceUpdate)
                    return
                }

                // 2. Check User Suspension (only if logged in)
                if (currentUser != null) {
                    database.getReference("userProfiles").child(currentUser.uid).child("status")
                        .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                            override fun onDataChange(userSnapshot: com.google.firebase.database.DataSnapshot) {
                                val status = userSnapshot.getValue(String::class.java)
                                if (status == "suspended") {
                                    _appStatus.postValue(AppStatus.Suspended)
                                } else {
                                    _appStatus.postValue(AppStatus.Active)
                                }
                            }

                            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                // Handle error
                            }
                        })
                } else {
                    _appStatus.postValue(AppStatus.Active)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle error
            }
        })
    }

    private fun isUpdateRequired(currentVersion: String?, serverVersion: String): Boolean {
        try {
            val currentParts = currentVersion!!.split(".").map { it.toIntOrNull() ?: 0 }
            val serverParts = serverVersion.split(".").map { it.toIntOrNull() ?: 0 }

            val length = maxOf(currentParts.size, serverParts.size)
            
            for (i in 0 until length) {
                val currentPart = if (i < currentParts.size) currentParts[i] else 0
                val serverPart = if (i < serverParts.size) serverParts[i] else 0
                
                if (serverPart > currentPart) {
                    return true
                } else if (serverPart < currentPart) {
                    return false
                }
            }
            return false // Versions are equal
        } catch (e: Exception) {
            return false
        }
    }
}

sealed class AppStatus {
    object Active : AppStatus()
    object Suspended : AppStatus()
    object Maintenance : AppStatus()
    object ForceUpdate : AppStatus()
}

/**
 * Data class to represent an announcement with course context
 */
@androidx.annotation.Keep
data class AnnouncementItem(
    val id: String,
    val courseName: String,
    val courseId: String,
    val author: String,
    val message: String,
    val createdAt: Long,
    val imageUrl: String
)
