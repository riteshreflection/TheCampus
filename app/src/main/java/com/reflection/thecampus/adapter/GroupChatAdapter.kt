package com.reflection.thecampus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.GroupChatMessage
import java.text.SimpleDateFormat
import java.util.*

class GroupChatAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (GroupChatMessage, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ADMIN_USER_ID = "5eNyiVGn2SeOyO4RrFBjk3HG8B52"
        
        fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date(timestamp))
        }
        
        fun formatReactions(reactions: Map<String, String>): String {
            val grouped = reactions.values.groupingBy { it }.eachCount()
            return grouped.entries.joinToString(" ") { "${it.key} ${it.value}" }
        }
    }

    private val messages = mutableListOf<GroupChatMessage>()
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    fun setMessages(newMessages: List<GroupChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: GroupChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun getMessageAt(position: Int): GroupChatMessage? {
        return if (position in messages.indices) messages[position] else null
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message, onMessageLongClick)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message, onMessageLongClick)
        }
    }

    override fun getItemCount() = messages.size

    class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val tvReactions: TextView = view.findViewById(R.id.tvReactions)
        private val cardReactions: CardView = view.findViewById(R.id.cardReactions)
        private val layoutReply: LinearLayout = view.findViewById(R.id.layoutReply)
        private val tvReplySender: TextView = view.findViewById(R.id.tvReplySender)
        private val tvReplyText: TextView = view.findViewById(R.id.tvReplyText)

        fun bind(message: GroupChatMessage, onLongClick: (GroupChatMessage, View) -> Unit) {
            tvMessage.text = message.text
            // Make URLs clickable with aqua color
            android.text.util.Linkify.addLinks(tvMessage, android.text.util.Linkify.ALL)
            tvMessage.setLinkTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.aqua_link))
            tvMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            
            tvTimestamp.text = formatTime(message.timestamp)
            
            // Handle reply
            if (message.replyToId != null && message.replyToText != null) {
                layoutReply.visibility = View.VISIBLE
                tvReplySender.text = message.replyToSender ?: "Unknown"
                tvReplyText.text = message.replyToText
            } else {
                layoutReply.visibility = View.GONE
            }
            
            // Handle reactions - FIXED to use cardReactions
            if (message.reactions.isNotEmpty()) {
                cardReactions.visibility = View.VISIBLE
                tvReactions.text = formatReactions(message.reactions)
                // Make clickable to show bottom sheet
                cardReactions.setOnClickListener {
                    onLongClick(message, itemView)
                }
            } else {
                cardReactions.visibility = View.GONE
            }
            
            // Long click for reactions
            itemView.setOnLongClickListener {
                onLongClick(message, itemView)
                true
            }
        }
    }

    class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        private val ivAdminBadge: android.widget.ImageView = view.findViewById(R.id.ivAdminBadge)
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val tvReactions: TextView = view.findViewById(R.id.tvReactions)
        private val cardReactions: CardView = view.findViewById(R.id.cardReactions)
        private val layoutReply: LinearLayout = view.findViewById(R.id.layoutReply)
        private val tvReplySender: TextView = view.findViewById(R.id.tvReplySender)
        private val tvReplyText: TextView = view.findViewById(R.id.tvReplyText)

        fun bind(message: GroupChatMessage, onLongClick: (GroupChatMessage, View) -> Unit) {
            // Check if sender is admin
            if (message.senderId == ADMIN_USER_ID) {
                tvSenderName.text = "Admin"
                ivAdminBadge.visibility = android.view.View.VISIBLE
            } else {
                tvSenderName.text = message.senderName
                ivAdminBadge.visibility = android.view.View.GONE
            }
            
            tvMessage.text = message.text
            // Make URLs clickable with aqua color
            android.text.util.Linkify.addLinks(tvMessage, android.text.util.Linkify.ALL)
            tvMessage.setLinkTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.aqua_link))
            tvMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            
            tvTimestamp.text = formatTime(message.timestamp)
            
            // Handle reply
            if (message.replyToId != null && message.replyToText != null) {
                layoutReply.visibility = View.VISIBLE
                tvReplySender.text = message.replyToSender ?: "Unknown"
                tvReplyText.text = message.replyToText
            } else {
                layoutReply.visibility = View.GONE
            }
            
            // Handle reactions - FIXED to use cardReactions
            if (message.reactions.isNotEmpty()) {
                cardReactions.visibility = View.VISIBLE
                tvReactions.text = formatReactions(message.reactions)
                // Make clickable to show bottom sheet
                cardReactions.setOnClickListener {
                    onLongClick(message, itemView)
                }
            } else {
                cardReactions.visibility = View.GONE
            }
            
            // Long click for reactions
            itemView.setOnLongClickListener {
                onLongClick(message, itemView)
                true
            }
        }
    }

}
