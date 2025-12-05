package com.reflection.thecampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reflection.thecampus.AnnouncementItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AnnouncementAdapter(
    private var announcements: List<AnnouncementItem>
) : RecyclerView.Adapter<AnnouncementViewHolder>() {
    
    fun updateAnnouncements(newAnnouncements: List<AnnouncementItem>) {
        announcements = newAnnouncements
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]
        
        holder.tvCourseName.text = announcement.courseName
        holder.tvAuthor.text = "By: ${announcement.author}"
        holder.tvMessage.text = announcement.message
        // Make URLs, emails, and phone numbers clickable with aqua color
        android.text.util.Linkify.addLinks(holder.tvMessage, android.text.util.Linkify.ALL)
        holder.tvMessage.setLinkTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.aqua_link))
        holder.tvMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        
        // Simplified time setting to avoid potential inference issues
        val timeString = getTimeAgo(announcement.createdAt)
        holder.tvTime.text = timeString

        // Load image if available
        if (announcement.imageUrl.isNotEmpty()) {
            holder.ivImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(announcement.imageUrl)
                .into(holder.ivImage)
        } else {
            holder.ivImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = announcements.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

class AnnouncementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
    val tvTime: TextView = view.findViewById(R.id.tvTime)
    val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
    val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    val ivImage: ImageView = view.findViewById(R.id.ivAnnouncementImage)
}
