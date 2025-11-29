package com.reflection.thecampus.utils

import android.content.Context
import android.provider.Settings
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

object FCMManager {

    private const val PREF_NAME = "FCM_PREF"
    private const val KEY_DEVICE_ID = "device_id"

    fun saveToken(context: Context, userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Get or Generate Device ID
            val deviceId = getDeviceId(context)

            val database = FirebaseDatabase.getInstance()
            val tokenRef = database.getReference("fcmTokens").child(userId).child(deviceId)

            val tokenData = mapOf(
                "deviceId" to deviceId,
                "platform" to "android",
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )

            tokenRef.setValue(tokenData)
        }
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            // Try to use ANDROID_ID, fallback to UUID
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            
            if (deviceId.isNullOrEmpty() || deviceId == "9774d56d682e549c") { // Known bad ID
                deviceId = UUID.randomUUID().toString()
            }
            
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        return deviceId!!
    }
}
