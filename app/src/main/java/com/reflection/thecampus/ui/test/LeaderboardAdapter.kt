package com.reflection.thecampus.ui.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.LeaderboardEntry
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.TimeUnit

class LeaderboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    
    companion object {
        private const val VIEW_TYPE_PREMIUM = 1
        private const val VIEW_TYPE_REGULAR = 2
        private const val VIEW_TYPE_SEPARATOR = 3
    }
    
    fun submitList(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LeaderboardEntry -> {
                val entry = items[position] as LeaderboardEntry
                if (entry.isPremium()) VIEW_TYPE_PREMIUM else VIEW_TYPE_REGULAR
            }
            else -> VIEW_TYPE_SEPARATOR // Assuming String or specific Separator object
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PREMIUM -> {
                val view = inflater.inflate(R.layout.item_leaderboard_premium, parent, false)
                PremiumViewHolder(view)
            }
            VIEW_TYPE_SEPARATOR -> {
                val view = inflater.inflate(R.layout.item_leaderboard_separator, parent, false)
                SeparatorViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_leaderboard_regular, parent, false)
                RegularViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PremiumViewHolder -> holder.bind(items[position] as LeaderboardEntry)
            is RegularViewHolder -> holder.bind(items[position] as LeaderboardEntry)
            is SeparatorViewHolder -> { /* Static content */ }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class PremiumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardPremium)
        private val badgeContainer: View = itemView.findViewById(R.id.badgeContainer)
        private val badgeText: TextView = itemView.findViewById(R.id.tvBadgeText)
        private val badgeIcon: android.widget.ImageView = itemView.findViewById(R.id.ivBadgeIcon)
        private val avatar: android.widget.ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvUserName)
        private val score: TextView = itemView.findViewById(R.id.tvScore)
        private val time: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(entry: LeaderboardEntry) {
            name.text = entry.userName
            score.text = String.format("%.2f", entry.score)
            time.text = formatTime(entry.timeTaken)
            badgeText.text = entry.getPlaceText()

            // Set styling based on rank
            val context = itemView.context
            when (entry.rank) {
                1 -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rank_1_bg))
                    card.strokeColor = ContextCompat.getColor(context, R.color.rank_1_stroke)
                    badgeContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_rank_1)
                    badgeIcon.setImageResource(R.drawable.trophy_base_svgrepo_com)
                }
                2 -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rank_2_bg))
                    card.strokeColor = ContextCompat.getColor(context, R.color.rank_2_stroke)
                    badgeContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_rank_2)
                    badgeIcon.setImageResource(R.drawable.medal_svgrepo_com)
                }
                3 -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rank_3_bg))
                    card.strokeColor = ContextCompat.getColor(context, R.color.rank_3_stroke)
                    badgeContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_rank_3)
                    badgeIcon.setImageResource(R.drawable.award_svgrepo_com)
                }
            }

            // Load Avatar
            if (!entry.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView)
                    .load(entry.userAvatar)
                    .placeholder(R.drawable.ic_profile_placeholder) 
                    .error(R.drawable.ic_profile_placeholder)
                    .into(avatar)
            } else {
                 avatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    inner class RegularViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.containerRegular)
        private val rank: TextView = itemView.findViewById(R.id.tvRank)
        private val avatar: android.widget.ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvUserName)
        private val score: TextView = itemView.findViewById(R.id.tvScore)
        private val time: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(entry: LeaderboardEntry) {
            rank.text = "#${entry.rank}"
            name.text = if (entry.isCurrentUser) "${entry.userName} (You)" else entry.userName
            score.text = String.format("%.2f", entry.score)
            time.text = formatTime(entry.timeTaken)

            // Highlighting for current user
            if (entry.isCurrentUser) {
                container.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_current_user_highlight)
            } else {
                container.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_leaderboard_regular)
            }

            // Load Avatar
            if (!entry.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView)
                    .load(entry.userAvatar)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(avatar)
            } else {
                avatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    inner class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            String.format("%dm %ds", minutes, remainingSeconds)
        } else {
            String.format("%ds", remainingSeconds)
        }
    }
}
