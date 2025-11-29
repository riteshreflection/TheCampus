package com.reflection.thecampus.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.LoginActivity
import com.reflection.thecampus.data.model.LoginHistory
import java.util.UUID

object SessionManager {

    private const val PREF_NAME = "AppSession"
    private const val KEY_SESSION_ID = "session_id"

    fun createSession(context: Context, userId: String) {
        val sessionId = "android_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
        
        // 1. Save locally
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply()

        // 2. Save to Firebase
        val database = FirebaseDatabase.getInstance()
        val sessionRef = database.getReference("users").child(userId).child("session").child("android")
        sessionRef.child("activeSessionId").setValue(sessionId)

        // 3. Log Login History
        val historyId = database.getReference("usersLoginhistory").child(userId).push().key ?: return
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        val loginHistory = LoginHistory(
            id = historyId,
            userId = userId,
            deviceId = sessionId, // Using session ID as device ID for this context, or could use ANDROID_ID
            deviceModel = deviceModel,
            osVersion = osVersion,
            loginTime = System.currentTimeMillis(),
            appVersion = appVersion
        )

        database.getReference("usersLoginhistory").child(userId).child(historyId).setValue(loginHistory)
    }

    fun checkSession(context: Context, onSessionValid: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onSessionValid(true) // Not logged in, so no session conflict
            return
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val localSessionId = prefs.getString(KEY_SESSION_ID, null)

        if (localSessionId == null) {
            // No local session but user is logged in. This might be a fresh install or cleared data.
            // We should probably logout or create a new session. 
            // For security, let's logout.
            logout(context, "Session expired. Please login again.")
            onSessionValid(false)
            return
        }

        val userId = currentUser.uid
        val database = FirebaseDatabase.getInstance()
        val sessionRef = database.getReference("users").child(userId).child("session").child("android").child("activeSessionId")

        sessionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serverSessionId = snapshot.getValue(String::class.java)
                
                if (serverSessionId != null && serverSessionId != localSessionId) {
                    // Session mismatch
                    logout(context, "You have logged in on another device. Please login again.")
                    onSessionValid(false)
                } else {
                    // Session valid or no server session (first login?)
                    // If no server session, we might want to set it? 
                    // But createSession should have done that. 
                    // If serverSessionId is null, it's weird if we have a local one.
                    // Let's assume valid if match or server is null (maybe deleted manually)
                    onSessionValid(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.e(error.toException(), "Session check failed")
                
                // Check if network is available
                if (context.isNetworkAvailable()) {
                    // Network available but Firebase error - invalid session for security
                    logout(context, "Session verification failed. Please login again.")
                    onSessionValid(false)
                } else {
                    // No network - allow offline usage but user can't perform sensitive operations
                    onSessionValid(true)
                }
            }
        })
    }

    fun logout(context: Context, message: String? = null) {
        // Clear local session
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SESSION_ID).apply()

        // Sign out Firebase
        FirebaseAuth.getInstance().signOut()

        // Show message
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

        // Redirect to Login
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
