package com.reflection.thecampus.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes

/**
 * Helper to apply theme to activities (Dark mode only - Black and White design)
 */
object ThemeHelper {
    
    /**
     * Apply the base theme to an activity
     * Call this BEFORE setContentView() in onCreate()
     * Note: Material3 DayNight theme automatically handles dark mode
     */
    fun applyTheme(activity: Activity) {
        // Base theme is already set in AndroidManifest
        // Material3 automatically applies dark mode colors when system is in dark mode
        // No additional theme overlay needed for pure black/white design
        activity.setTheme(com.reflection.thecampus.R.style.Theme_TheCampus)
    }
    
    /**
     * Check if the system is in dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
