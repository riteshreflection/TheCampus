package com.reflection.thecampus.utils

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.reflection.thecampus.EditProfileActivity
import com.reflection.thecampus.LoginPromptBottomSheet
import com.reflection.thecampus.ProfileCompletionBottomSheet
import com.reflection.thecampus.UserProfile

object ProfileValidator {
    
    /**
     * Checks if user is logged in and has a complete profile
     * @param activity The calling activity
     * @param onProfileComplete Callback when profile is complete
     */
    fun validateProfileForFreeAccess(
        activity: FragmentActivity,
        onProfileComplete: () -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        // Check if user is logged in
        if (currentUser == null) {
            showLoginPrompt(activity)
            return
        }
        
        // Check if profile is complete
        checkProfileCompletion(activity, currentUser.uid) { isComplete ->
            if (isComplete) {
                onProfileComplete()
            } else {
                showProfileCompletionPrompt(activity)
            }
        }
    }
    
    /**
     * Checks if the user's profile has all required fields
     */
    private fun checkProfileCompletion(
        activity: Activity,
        userId: String,
        callback: (Boolean) -> Unit
    ) {
        FirebaseDatabase.getInstance()
            .getReference("userProfiles")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.getValue(UserProfile::class.java)
                
                // Profile is complete if all required fields are filled
                val isComplete = profile != null &&
                        !profile.fullName.isNullOrBlank() &&
                        !profile.email.isNullOrBlank() &&
                        !profile.mobileNumber.isNullOrBlank()
                
                callback(isComplete)
            }
            .addOnFailureListener {
                // If we can't fetch profile, assume it's incomplete
                callback(false)
            }
    }
    
    private fun showLoginPrompt(activity: FragmentActivity) {
        val loginSheet = LoginPromptBottomSheet()
        loginSheet.show(activity.supportFragmentManager, LoginPromptBottomSheet.TAG)
    }
    
    private fun showProfileCompletionPrompt(activity: FragmentActivity) {
        val profileSheet = ProfileCompletionBottomSheet()
        profileSheet.show(activity.supportFragmentManager, ProfileCompletionBottomSheet.TAG)
    }
}
