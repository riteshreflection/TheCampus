package com.reflection.thecampus.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.GroupChatMessage

class MessageActionsBottomSheet(
    private val message: GroupChatMessage,
    private val isOwnMessage: Boolean,
    private val onReactionToggle: (String) -> Unit,
    private val onReply: () -> Unit,
    private val onEdit: (() -> Unit)? = null,
    private val onDelete: (() -> Unit)? = null,
    private val onReport: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_message_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set message preview
        view.findViewById<TextView>(R.id.tvSenderName).text = message.senderName
        view.findViewById<TextView>(R.id.tvMessagePreview).text = message.text

        // Setup reactions
        setupReactions(view)

        // Setup actions
        setupActions(view)
    }

    private fun setupReactions(view: View) {
        val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🔥", "🎉")
        val reactionButtons = listOf(
            R.id.btnReaction1, R.id.btnReaction2, R.id.btnReaction3,
            R.id.btnReaction4, R.id.btnReaction5, R.id.btnReaction6,
            R.id.btnReaction7
        )

        // Get user's current reactions
        val userReactions = message.reactions.filter { it.key == currentUserId }.values.toSet()

        reactionButtons.forEachIndexed { index, buttonId ->
            val button = view.findViewById<TextView>(buttonId)
            val emoji = reactions[index]
            
            // Highlight if user already reacted with this emoji
            button.isSelected = userReactions.contains(emoji)
            
            button.setOnClickListener {
                // Toggle reaction
                onReactionToggle(emoji)
                dismiss()
            }
        }
    }

    private fun setupActions(view: View) {
        // Copy
        view.findViewById<LinearLayout>(R.id.btnCopy).setOnClickListener {
            copyToClipboard()
            dismiss()
        }

        // Reply
        view.findViewById<LinearLayout>(R.id.btnReply).setOnClickListener {
            onReply()
            dismiss()
        }

        // Edit (only for own messages)
        val btnEdit = view.findViewById<LinearLayout>(R.id.btnEdit)
        if (isOwnMessage && onEdit != null) {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                onEdit.invoke()
                dismiss()
            }
        }

        // Delete (only for own messages)
        val btnDelete = view.findViewById<LinearLayout>(R.id.btnDelete)
        if (isOwnMessage && onDelete != null) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                onDelete.invoke()
                dismiss()
            }
        }

        // Report (only for others' messages)
        val btnReport = view.findViewById<LinearLayout>(R.id.btnReport)
        if (!isOwnMessage && onReport != null) {
            btnReport.visibility = View.VISIBLE
            btnReport.setOnClickListener {
                onReport.invoke()
                dismiss()
            }
        }
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", message.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    companion object {
        const val TAG = "MessageActionsBottomSheet"
    }
}
