package com.reflection.thecampus.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Utility to get black/white colors for the current theme mode
 */
object DynamicThemeUtils {
    
    /**
     * Get the current primary color (Black in light mode, White in dark mode)
     */
    @ColorInt
    fun getPrimaryColor(context: Context): Int {
        return if (isDarkMode(context)) {
            Color.WHITE // #FFFFFF
        } else {
            Color.BLACK // #000000
        }
    }
    
    /**
     * Create a ColorStateList for the primary color
     */
    fun getPrimaryColorStateList(context: Context): ColorStateList {
        val color = getPrimaryColor(context)
        return ColorStateList.valueOf(color)
    }
    
    /**
     * Apply primary color to a MaterialButton
     */
    fun applyToButton(context: Context, button: com.google.android.material.button.MaterialButton) {
        val colorStateList = getPrimaryColorStateList(context)
        button.backgroundTintList = colorStateList
    }
    
    /**
     * Apply primary color to a FloatingActionButton
     */
    fun applyToFAB(context: Context, fab: com.google.android.material.floatingactionbutton.FloatingActionButton) {
        val colorStateList = getPrimaryColorStateList(context)
        fab.backgroundTintList = colorStateList
    }
    
    /**
     * Apply primary color to text
     */
    fun applyToTextView(context: Context, textView: android.widget.TextView) {
        textView.setTextColor(getPrimaryColor(context))
    }
    
    /**
     * Get a gray color for secondary elements
     */
    @ColorInt
    fun getSecondaryColor(context: Context): Int {
        return if (isDarkMode(context)) {
            0xFFBDBDBD.toInt() // Gray 300 in dark mode
        } else {
            0xFF424242.toInt() // Gray 700 in light mode
        }
    }
    
    /**
     * Check if the system is in dark mode
     */
    private fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
