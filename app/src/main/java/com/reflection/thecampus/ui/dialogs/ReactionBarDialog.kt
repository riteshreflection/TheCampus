package com.reflection.thecampus.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.reflection.thecampus.R

class ReactionBarDialog(
    context: Context,
    private val onReactionSelected: (String) -> Unit,
    private val onEdit: (() -> Unit)? = null,
    private val onDelete: (() -> Unit)? = null
) : Dialog(context) {

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_bar, null)
        setContentView(view)

        // Make dialog background transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)

        setupReactions(view)
    }

    private fun setupReactions(view: View) {
        val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
        val emojiViews = listOf(
            view.findViewById<TextView>(R.id.emoji1),
            view.findViewById<TextView>(R.id.emoji2),
            view.findViewById<TextView>(R.id.emoji3),
            view.findViewById<TextView>(R.id.emoji4),
            view.findViewById<TextView>(R.id.emoji5),
            view.findViewById<TextView>(R.id.emoji6)
        )

        emojiViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                onReactionSelected(emojis[index])
                dismiss()
            }
        }

        view.findViewById<TextView>(R.id.emojiMore).setOnClickListener {
            // Show more emojis or action menu
            showActionMenu()
        }
    }

    private fun showActionMenu() {
        dismiss()
        // Show action menu with edit/delete options
        val builder = android.app.AlertDialog.Builder(context)
        val options = mutableListOf<String>()
        
        if (onEdit != null) options.add("Edit")
        if (onDelete != null) options.add("Delete")
        options.add("Cancel")

        builder.setItems(options.toTypedArray()) { dialog, which ->
            when (options[which]) {
                "Edit" -> onEdit?.invoke()
                "Delete" -> onDelete?.invoke()
            }
            dialog.dismiss()
        }
        builder.show()
    }
}
