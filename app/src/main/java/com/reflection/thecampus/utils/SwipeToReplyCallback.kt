package com.reflection.thecampus.utils

import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SwipeToReplyCallback(
    private val onSwipeToReply: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val swipeThreshold = 0.3f
    private val maxSwipeDistance = 150f

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            // Trigger haptic feedback
            viewHolder.itemView.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            onSwipeToReply(position)
        }
        
        // IMPORTANT: Reset the view position after swipe is detected
        // This is handled by notifying the adapter
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        // Limit swipe distance and add resistance
        val clampedDx = when {
            dX < 0 -> {
                // Swiping left
                val progress = abs(dX) / maxSwipeDistance
                if (progress > 1f) {
                    -maxSwipeDistance
                } else {
                    dX
                }
            }
            else -> 0f
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 10 // Make it harder to accidentally swipe away
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Reset view to original position
        viewHolder.itemView.translationX = 0f
    }
}
