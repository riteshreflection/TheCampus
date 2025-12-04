package com.reflection.thecampus.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes

/**
 * Helper to apply dynamic theme overlay to activities
 */
object ThemeHelper {
    
    /**
     * Apply the saved theme to an activity
     * Call this BEFORE setContentView() in onCreate()
     */
    fun applyTheme(activity: Activity) {
        val theme = ThemeManager.getCurrentTheme(activity)
        val isDarkMode = isDarkMode(activity)
        
        // Apply the appropriate theme overlay based on selected color
        val themeResId = getThemeResourceId(theme, isDarkMode)
        activity.setTheme(themeResId)
    }
    
    /**
     * Get the theme resource ID for a given color theme
     */
    @StyleRes
    private fun getThemeResourceId(theme: ThemeManager.ColorTheme, isDarkMode: Boolean): Int {
        return when (theme) {
            ThemeManager.ColorTheme.PURPLE -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Purple_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Purple
            
            ThemeManager.ColorTheme.BLUE -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Blue_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Blue
            
            ThemeManager.ColorTheme.TEAL -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Teal_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Teal
            
            ThemeManager.ColorTheme.GREEN -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Green_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Green
            
            ThemeManager.ColorTheme.ORANGE -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Orange_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Orange
            
            ThemeManager.ColorTheme.RED -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Red_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Red
            
            ThemeManager.ColorTheme.PINK -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Pink_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Pink
            
            ThemeManager.ColorTheme.INDIGO -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Indigo_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Indigo
            
            ThemeManager.ColorTheme.CYAN -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Cyan_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Cyan
            
            ThemeManager.ColorTheme.AMBER -> if (isDarkMode) 
                com.reflection.thecampus.R.style.Theme_TheCampus_Amber_Dark 
            else 
                com.reflection.thecampus.R.style.Theme_TheCampus_Amber
        }
    }
    
    private fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
