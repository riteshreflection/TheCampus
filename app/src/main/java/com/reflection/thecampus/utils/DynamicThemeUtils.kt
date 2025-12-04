package com.reflection.thecampus.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.material.color.MaterialColors

/**
 * Utility to apply dynamic theme colors to activities
 */
object DynamicThemeUtils {
    
    /**
     * Apply dynamic theme colors to an activity
     * Call this in onCreate() after setContentView()
     */
    fun applyDynamicTheme(activity: Activity) {
        val prefs = activity.getSharedPreferences("theme_runtime", Activity.MODE_PRIVATE)
        val primaryColor = prefs.getInt("runtime_primary_color", -1)
        
        if (primaryColor != -1) {
            // Apply to window
            activity.window.statusBarColor = adjustColorBrightness(primaryColor, 0.8f)
            
            // The theme colors will be applied through the theme system
            // Individual views can query the primary color if needed
        }
    }
    
    /**
     * Get the current runtime primary color
     */
    @ColorInt
    fun getPrimaryColor(activity: Activity): Int {
        val prefs = activity.getSharedPreferences("theme_runtime", Activity.MODE_PRIVATE)
        return prefs.getInt("runtime_primary_color", 0xFF6200EE.toInt())
    }
    
    /**
     * Adjust color brightness
     */
    @ColorInt
    private fun adjustColorBrightness(@ColorInt color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= factor
        return Color.HSVToColor(hsv)
    }
    
    /**
     * Create a ColorStateList for the primary color
     */
    fun getPrimaryColorStateList(activity: Activity): ColorStateList {
        val color = getPrimaryColor(activity)
        return ColorStateList.valueOf(color)
    }
    
    /**
     * Apply primary color to a MaterialButton
     */
    fun applyToButton(activity: Activity, button: com.google.android.material.button.MaterialButton) {
        val colorStateList = getPrimaryColorStateList(activity)
        button.backgroundTintList = colorStateList
    }
    
    /**
     * Apply primary color to a FloatingActionButton
     */
    fun applyToFAB(activity: Activity, fab: com.google.android.material.floatingactionbutton.FloatingActionButton) {
        val colorStateList = getPrimaryColorStateList(activity)
        fab.backgroundTintList = colorStateList
    }
    
    /**
     * Apply primary color to text
     */
    fun applyToTextView(activity: Activity, textView: android.widget.TextView) {
        textView.setTextColor(getPrimaryColor(activity))
    }
    
    /**
     * Get a lighter version of the primary color for backgrounds
     */
    @ColorInt
    fun getPrimaryColorLight(activity: Activity): Int {
        val color = getPrimaryColor(activity)
        return adjustColorBrightness(color, 1.3f)
    }
}
