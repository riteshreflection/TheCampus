package com.reflection.thecampus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.CourseChatPreview
import java.util.concurrent.TimeUnit

class CourseChatListAdapter(
    private val onChatClick: (CourseChatPreview) -> Unit
) : RecyclerView.Adapter<CourseChatListAdapter.ChatViewHolder>() {

    private var chats = listOf<CourseChatPreview>()

    fun submitList(newChats: List<CourseChatPreview>) {
        chats = newChats.sortedByDescending { it.lastMessageTime }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount() = chats.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCourseThumbnail: ImageView = itemView.findViewById(R.id.ivCourseThumbnail)
        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(chat: CourseChatPreview) {
            tvCourseName.text = chat.courseName
            
            // Format last message with sender name
            tvLastMessage.text = if (chat.lastMessageSender.isNotEmpty()) {
                "${chat.lastMessageSender}: ${chat.lastMessage}"
            } else {
                chat.lastMessage
            }
            
            // Format timestamp
            tvTimestamp.text = formatTimestamp(chat.lastMessageTime)
            
            // Load course thumbnail
            if (chat.courseImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(chat.courseImageUrl)
                    .placeholder(R.drawable.ic_book)
                    .into(ivCourseThumbnail)
            } else {
                ivCourseThumbnail.setImageResource(R.drawable.ic_book)
            }
            
            // Click listener
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "${minutes}m ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "${hours}h ago"
                }
                diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "${days}d ago"
                }
                else -> {
                    val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    date.format(java.util.Date(timestamp))
                }
            }
        }
    }
}
