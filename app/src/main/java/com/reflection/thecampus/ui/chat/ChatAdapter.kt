package com.reflection.thecampus.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val currentUserId: String) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun setMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isCurrentUser = message.senderId == currentUserId
        
        // Show date header if it's the first message or different day from previous
        val showDate = if (position == 0) {
            true
        } else {
            val prevMessage = messages[position - 1]
            !isSameDay(message.timestamp, prevMessage.timestamp)
        }
        
        holder.bind(message, isCurrentUser, showDate)
    }

    override fun getItemCount(): Int = messages.size

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)
        private val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
        private val cardMessage: CardView = itemView.findViewById(R.id.cardMessage)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ivReadStatus: ImageView = itemView.findViewById(R.id.ivReadStatus)

        fun bind(message: ChatMessage, isCurrentUser: Boolean, showDate: Boolean) {
            tvMessage.text = message.text
            tvTimestamp.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
            
            if (showDate) {
                tvDateHeader.visibility = View.VISIBLE
                tvDateHeader.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(message.timestamp))
            } else {
                tvDateHeader.visibility = View.GONE
            }

            val params = messageContainer.layoutParams as LinearLayout.LayoutParams
            if (isCurrentUser) {
                params.gravity = Gravity.END
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_primary))
                tvMessage.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                tvTimestamp.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                ivReadStatus.visibility = View.VISIBLE
                ivReadStatus.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.white))
                // Update read status icon based on isRead
                ivReadStatus.alpha = if (message.isRead) 1.0f else 0.5f
            } else {
                params.gravity = Gravity.START
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                tvMessage.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                tvTimestamp.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                ivReadStatus.visibility = View.GONE
            }
            messageContainer.layoutParams = params
        }
    }
}
