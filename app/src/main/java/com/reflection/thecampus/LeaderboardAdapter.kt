package com.reflection.thecampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.data.model.LeaderboardEntry

class LeaderboardAdapter(
    private var entries: List<LeaderboardEntry>,
    private val currentUserId: String?,
    private var metricType: MetricType
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    enum class MetricType {
        POINTS, STREAK, ACCURACY, TESTS, LEVEL
    }

    fun updateData(newEntries: List<LeaderboardEntry>, newMetricType: MetricType) {
        entries = newEntries
        metricType = newMetricType
        notifyDataSetChanged()
    }

    fun submitList(newEntries: List<LeaderboardEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount() = entries.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardRoot: CardView = itemView.findViewById(R.id.cardRoot)
        private val rankIcon: TextView = itemView.findViewById(R.id.rankIcon)
        private val rankBadge: TextView = itemView.findViewById(R.id.rankBadge)
        private val avatar: TextView = itemView.findViewById(R.id.avatar)
        private val displayName: TextView = itemView.findViewById(R.id.displayName)
        private val championBadge: TextView = itemView.findViewById(R.id.championBadge)
        private val level: TextView = itemView.findViewById(R.id.level)
        private val totalTests: TextView = itemView.findViewById(R.id.totalTests)
        private val averageScore: TextView = itemView.findViewById(R.id.averageScore)
        private val streakIndicator: TextView = itemView.findViewById(R.id.streakIndicator)
        private val metricValue: TextView = itemView.findViewById(R.id.metricValue)
        private val metricLabel: TextView = itemView.findViewById(R.id.metricLabel)

        fun bind(entry: LeaderboardEntry) {
            val isCurrentUser = entry.userId == currentUserId

            // Rank icon and badge
            rankIcon.text = getRankIcon(entry.rank)
            rankBadge.text = "#${entry.rank}"
            rankBadge.setBackgroundResource(getRankBadgeBackground(entry.rank))

            // Avatar - show first letter of name
            avatar.text = entry.displayName.firstOrNull()?.uppercase() ?: "?"

            // User info
            displayName.text = entry.displayName
            level.text = "Level ${entry.level}"
            totalTests.text = "${entry.totalTests} tests"
            averageScore.text = "${String.format("%.1f", entry.averageScore)}%"

            // Streak indicator
            if (entry.currentStreak > 0) {
                streakIndicator.visibility = View.VISIBLE
                streakIndicator.text = "⚡ ${entry.currentStreak} day streak"
            } else {
                streakIndicator.visibility = View.GONE
            }

            // Champion badge for rank 1
            championBadge.visibility = if (entry.rank == 1) View.VISIBLE else View.GONE

            // Metric value (right side)
            val (metricVal, metricLbl) = getMetricDisplay(entry)
            metricValue.text = metricVal
            metricLabel.text = metricLbl

            // Highlight current user
            if (isCurrentUser) {
                cardRoot.setCardBackgroundColor(0xFFF5F7FA.toInt())
                val typedValue = android.util.TypedValue()

            } else {
                cardRoot.setCardBackgroundColor(0xFFFFFFFF.toInt())
            }
        }

        private fun getMetricDisplay(entry: LeaderboardEntry): Pair<String, String> {
            return when (metricType) {
                MetricType.POINTS -> {
                    val formatted = if (entry.points >= 1000) {
                        String.format("%,d", entry.points)
                    } else {
                        entry.points.toString()
                    }
                    formatted to "points"
                }
                MetricType.STREAK -> entry.currentStreak.toString() to "days"
                MetricType.ACCURACY -> "${String.format("%.1f", entry.averageScore)}%" to "accuracy"
                MetricType.TESTS -> entry.totalTests.toString() to "tests"
                MetricType.LEVEL -> entry.level.toString() to "level"
            }
        }

        private fun getRankIcon(rank: Int): String {
            return when {
                rank == 1 -> "👑"
                rank == 2 -> "🥈"
                rank == 3 -> "🥉"
                rank <= 10 -> "🏆"
                rank <= 50 -> "⭐"
                else -> "👤"
            }
        }

        private fun getRankBadgeBackground(rank: Int): Int {
            return when {
                rank == 1 -> R.drawable.bg_rank_gold
                rank == 2 -> R.drawable.bg_rank_silver
                rank == 3 -> R.drawable.bg_rank_bronze
                rank <= 10 -> R.drawable.bg_rank_blue
                rank <= 50 -> R.drawable.bg_rank_purple
                else -> R.drawable.bg_rank_default
            }
        }
    }
}
