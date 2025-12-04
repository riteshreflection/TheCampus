package com.reflection.thecampus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TheCampusApplication : Application() {

    companion object {
        lateinit var instance: TheCampusApplication
            private set
        
        const val CHANNEL_ID_GENERAL = "general_channel"
        const val CHANNEL_ID_UPDATES = "updates_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeLogging()
        initializeCrashlytics()
        createNotificationChannels()
        applyTheme()
    }
    
    private fun applyTheme() {
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        val primaryColor = com.reflection.thecampus.utils.ThemeManager.getPrimaryColor(this, isDarkMode)
        
        // Store the color in a way that can be accessed by themes
        val prefs = getSharedPreferences("theme_runtime", MODE_PRIVATE)
        prefs.edit().putInt("runtime_primary_color", primaryColor).apply()
    }
    
    private fun initializeLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        } else {
            // Plant production tree that logs to Crashlytics
            timber.log.Timber.plant(object : timber.log.Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                        // Log errors and warnings to Crashlytics
                        if (t != null) {
                            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(t)
                        } else {
                            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("$tag: $message")
                        }
                    }
                }
            })
        }
    }
    
    private fun initializeCrashlytics() {
        // Crashlytics is auto-initialized, but we can configure it here
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                "Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important app updates"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(generalChannel)
            manager.createNotificationChannel(updatesChannel)
        }
    }
}
