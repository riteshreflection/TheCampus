package com.reflection.thecampus.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Helper object to handle window insets consistently across the app.
 * This ensures content doesn't get obscured by system bars (status bar, navigation bar).
 */
object WindowInsetsHelper {
    
    /**
     * Apply system bar insets to a view by adding padding.
     * This ensures the view's content is not obscured by system bars.
     * 
     * @param view The view to apply insets to
     * @param left Whether to apply left inset (default: false)
     * @param top Whether to apply top inset (default: true)
     * @param right Whether to apply right inset (default: false)
     * @param bottom Whether to apply bottom inset (default: true)
     */
    fun applySystemBarInsets(
        view: View,
        left: Boolean = false,
        top: Boolean = true,
        right: Boolean = false,
        bottom: Boolean = true
    ) {
        val initialPaddingLeft = view.paddingLeft
        val initialPaddingTop = view.paddingTop
        val initialPaddingRight = view.paddingRight
        val initialPaddingBottom = view.paddingBottom
        
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            v.updatePadding(
                left = if (left) initialPaddingLeft + insets.left else initialPaddingLeft,
                top = if (top) initialPaddingTop + insets.top else initialPaddingTop,
                right = if (right) initialPaddingRight + insets.right else initialPaddingRight,
                bottom = if (bottom) initialPaddingBottom + insets.bottom else initialPaddingBottom
            )
            
            windowInsets
        }
    }
    
    /**
     * Apply only top inset (status bar) to a view.
     * Useful for views that should respect the status bar but not the navigation bar.
     */
    fun applyTopInset(view: View) {
        applySystemBarInsets(view, left = false, top = true, right = false, bottom = false)
    }
    
    /**
     * Apply only bottom inset (navigation bar) to a view.
     * Useful for views that should respect the navigation bar but not the status bar.
     */
    fun applyBottomInset(view: View) {
        applySystemBarInsets(view, left = false, top = false, right = false, bottom = true)
    }
    
    /**
     * Apply margin instead of padding for system bar insets.
     * Useful when you want the background to extend but content to be inset.
     */
    fun applySystemBarInsetsAsMargin(
        view: View,
        left: Boolean = false,
        top: Boolean = true,
        right: Boolean = false,
        bottom: Boolean = true
    ) {
        val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        
        val initialMarginLeft = layoutParams.leftMargin
        val initialMarginTop = layoutParams.topMargin
        val initialMarginRight = layoutParams.rightMargin
        val initialMarginBottom = layoutParams.bottomMargin
        
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val newLayoutParams = v.layoutParams as ViewGroup.MarginLayoutParams
            newLayoutParams.leftMargin = if (left) initialMarginLeft + insets.left else initialMarginLeft
            newLayoutParams.topMargin = if (top) initialMarginTop + insets.top else initialMarginTop
            newLayoutParams.rightMargin = if (right) initialMarginRight + insets.right else initialMarginRight
            newLayoutParams.bottomMargin = if (bottom) initialMarginBottom + insets.bottom else initialMarginBottom
            v.layoutParams = newLayoutParams
            
            windowInsets
        }
    }
    
    /**
     * Get the current system bar insets.
     * Returns a WindowInsetsCompat object that can be queried for specific inset values.
     */
    fun getSystemBarInsets(view: View): WindowInsetsCompat? {
        return ViewCompat.getRootWindowInsets(view)
    }
}
