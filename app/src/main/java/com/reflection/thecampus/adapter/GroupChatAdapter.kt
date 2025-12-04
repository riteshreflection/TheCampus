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
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val tvReactions: TextView = view.findViewById(R.id.tvReactions)
        private val cardReactions: CardView = view.findViewById(R.id.cardReactions)
        private val layoutReply: LinearLayout = view.findViewById(R.id.layoutReply)
        private val tvReplySender: TextView = view.findViewById(R.id.tvReplySender)
        private val tvReplyText: TextView = view.findViewById(R.id.tvReplyText)

        fun bind(message: GroupChatMessage, onLongClick: (GroupChatMessage, View) -> Unit) {
            tvSenderName.text = message.senderName
            tvMessage.text = message.text
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

    companion object {
        fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        
        fun formatReactions(reactions: Map<String, String>): String {
            val grouped = reactions.values.groupingBy { it }.eachCount()
            return grouped.entries.joinToString(" ") { "${it.key} ${it.value}" }
        }
    }
}
