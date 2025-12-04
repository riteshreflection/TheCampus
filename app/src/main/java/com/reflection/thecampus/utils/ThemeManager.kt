package com.reflection.thecampus.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ColorInt

/**
 * Manages theme preferences and color customization
 */
object ThemeManager {
    
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_PRIMARY_COLOR = "primary_color"
    private const val KEY_THEME_NAME = "theme_name"
    
    /**
     * Predefined color palettes with Material Design inspired colors
     */
    enum class ColorTheme(
        val themeName: String,
        @ColorInt val lightPrimary: Int,
        @ColorInt val darkPrimary: Int
    ) {
        PURPLE("Purple", 0xFF6200EE.toInt(), 0xFFBB86FC.toInt()),
        BLUE("Blue", 0xFF2196F3.toInt(), 0xFF64B5F6.toInt()),
        TEAL("Teal", 0xFF009688.toInt(), 0xFF4DB6AC.toInt()),
        GREEN("Green", 0xFF4CAF50.toInt(), 0xFF81C784.toInt()),
        ORANGE("Orange", 0xFFFF9800.toInt(), 0xFFFFB74D.toInt()),
        RED("Red", 0xFFF44336.toInt(), 0xFFE57373.toInt()),
        PINK("Pink", 0xFFE91E63.toInt(), 0xFFF06292.toInt()),
        INDIGO("Indigo", 0xFF3F51B5.toInt(), 0xFF7986CB.toInt()),
        CYAN("Cyan", 0xFF00BCD4.toInt(), 0xFF4DD0E1.toInt()),
        AMBER("Amber", 0xFFFFC107.toInt(), 0xFFFFD54F.toInt())
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save selected theme
     */
    fun saveTheme(context: Context, theme: ColorTheme) {
        getPreferences(context).edit().apply {
            putString(KEY_THEME_NAME, theme.name)
            putInt(KEY_PRIMARY_COLOR, theme.lightPrimary)
            apply()
        }
    }
    
    /**
     * Get currently selected theme
     */
    fun getCurrentTheme(context: Context): ColorTheme {
        val themeName = getPreferences(context).getString(KEY_THEME_NAME, ColorTheme.PURPLE.name)
        return try {
            ColorTheme.valueOf(themeName ?: ColorTheme.PURPLE.name)
        } catch (e: IllegalArgumentException) {
            ColorTheme.PURPLE
        }
    }
    
    /**
     * Get primary color for current theme mode (light/dark)
     */
    @ColorInt
    fun getPrimaryColor(context: Context, isDarkMode: Boolean): Int {
        val theme = getCurrentTheme(context)
        return if (isDarkMode) theme.darkPrimary else theme.lightPrimary
    }
    
    /**
     * Check if a custom theme is set (different from default)
     */
    fun hasCustomTheme(context: Context): Boolean {
        return getPreferences(context).contains(KEY_THEME_NAME)
    }
    
    /**
     * Reset to default theme
     */
    fun resetToDefault(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
    
    /**
     * Get all available themes
     */
    fun getAllThemes(): List<ColorTheme> {
        return ColorTheme.values().toList()
    }
}
