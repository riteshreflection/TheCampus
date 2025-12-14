package com.reflection.thecampus.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Poll

class PollAdapter(
    private var polls: List<Poll>,
    private val currentUserId: String,
    private val onVoteSubmit: (Poll, List<String>) -> Unit
) : RecyclerView.Adapter<PollAdapter.PollViewHolder>() {

    private val expandedPollIds = mutableSetOf<String>()

    fun updatePolls(newPolls: List<Poll>) {
        polls = newPolls
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll, parent, false)
        return PollViewHolder(view)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        holder.bind(polls[position])
    }

    override fun getItemCount() = polls.size

    inner class PollViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPoll: CardView = itemView.findViewById(R.id.cardPoll)
        private val layoutHeader: View = itemView.findViewById(R.id.layoutHeader)
        private val tvPollTitle: TextView = itemView.findViewById(R.id.tvPollTitle)
        private val tvPollInfo: TextView = itemView.findViewById(R.id.tvPollInfo)
        private val ivExpandCollapse: ImageView = itemView.findViewById(R.id.ivExpandCollapse)
        private val layoutExpandable: View = itemView.findViewById(R.id.layoutExpandable)
        private val tvPollDescription: TextView = itemView.findViewById(R.id.tvPollDescription)
        private val rvPollOptions: RecyclerView = itemView.findViewById(R.id.rvPollOptions)
        private val btnSubmitVote: MaterialButton = itemView.findViewById(R.id.btnSubmitVote)
        private val rvPollResults: RecyclerView = itemView.findViewById(R.id.rvPollResults)
        private val layoutVotedIndicator: View = itemView.findViewById(R.id.layoutVotedIndicator)

        private var optionsAdapter: PollOptionsAdapter? = null
        private var resultsAdapter: PollResultsAdapter? = null

        fun bind(poll: Poll) {
            tvPollTitle.text = poll.title
            
            val totalVotes = poll.getTotalVotes()
            tvPollInfo.text = "$totalVotes ${if (totalVotes == 1) "vote" else "votes"}"

            // Show description if available
            if (poll.description.isNotEmpty()) {
                tvPollDescription.visibility = View.VISIBLE
                tvPollDescription.text = poll.description
            } else {
                tvPollDescription.visibility = View.GONE
            }

            val hasVoted = poll.hasUserVoted(currentUserId)
            val isExpanded = shouldBeExpanded(poll, hasVoted)

            // Setup expand/collapse
            layoutHeader.setOnClickListener {
                toggleExpand(poll.id)
            }

            // Update expand/collapse state with animation
            updateExpandState(isExpanded, false)

            if (hasVoted) {
                // Show results
                showResults(poll)
            } else {
                // Show voting options
                showVotingOptions(poll)
            }
        }

        private fun shouldBeExpanded(poll: Poll, hasVoted: Boolean): Boolean {
            return if (hasVoted) {
                // Collapsed by default if voted, but can be manually expanded
                expandedPollIds.contains(poll.id)
            } else {
                // Expanded by default if not voted, but can be manually collapsed
                !expandedPollIds.contains(poll.id)
            }
        }

        private fun toggleExpand(pollId: String) {
            val wasExpanded = expandedPollIds.contains(pollId)
            if (wasExpanded) {
                expandedPollIds.remove(pollId)
            } else {
                expandedPollIds.add(pollId)
            }
            
            // Animate the transition
            updateExpandState(!wasExpanded, true)
        }

        private fun updateExpandState(isExpanded: Boolean, animate: Boolean) {
            if (animate) {
                if (isExpanded) {
                    // Fold out animation
                    expand(layoutExpandable)
                    animateRotation(ivExpandCollapse, 180f)
                } else {
                    // Fold in animation
                    collapse(layoutExpandable)
                    animateRotation(ivExpandCollapse, 0f)
                }
            } else {
                // No animation, instant change
                if (isExpanded) {
                    layoutExpandable.visibility = View.VISIBLE
                    ivExpandCollapse.rotation = 180f
                } else {
                    layoutExpandable.visibility = View.GONE
                    ivExpandCollapse.rotation = 0f
                }
            }
        }

        private fun expand(view: View) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val targetHeight = view.measuredHeight

            view.layoutParams.height = 0
            view.visibility = View.VISIBLE

            val animator = ValueAnimator.ofInt(0, targetHeight)
            animator.addUpdateListener { animation ->
                view.layoutParams.height = animation.animatedValue as Int
                view.requestLayout()
            }
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.start()
        }

        private fun collapse(view: View) {
            val initialHeight = view.measuredHeight

            val animator = ValueAnimator.ofInt(initialHeight, 0)
            animator.addUpdateListener { animation ->
                view.layoutParams.height = animation.animatedValue as Int
                view.requestLayout()
            }
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.start()
            
            // Hide after animation
            view.postDelayed({
                view.visibility = View.GONE
            }, 300)
        }

        private fun animateRotation(view: View, targetRotation: Float) {
            val animator = ObjectAnimator.ofFloat(view, "rotation", view.rotation, targetRotation)
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.start()
        }

        private fun showVotingOptions(poll: Poll) {
            rvPollOptions.visibility = View.VISIBLE
            btnSubmitVote.visibility = View.VISIBLE
            rvPollResults.visibility = View.GONE
            layoutVotedIndicator.visibility = View.GONE

            // Setup options adapter
            if (optionsAdapter == null) {
                optionsAdapter = PollOptionsAdapter(poll.options, poll.type)
                rvPollOptions.layoutManager = LinearLayoutManager(itemView.context)
                rvPollOptions.isNestedScrollingEnabled = false
                rvPollOptions.adapter = optionsAdapter
            } else {
                optionsAdapter?.updateOptions(poll.options, poll.type)
            }

            // Submit vote button
            btnSubmitVote.setOnClickListener {
                val selectedOptions = optionsAdapter?.getSelectedOptions() ?: emptyList()
                if (selectedOptions.isNotEmpty()) {
                    onVoteSubmit(poll, selectedOptions)
                }
            }
        }

        private fun showResults(poll: Poll) {
            rvPollOptions.visibility = View.GONE
            btnSubmitVote.visibility = View.GONE
            rvPollResults.visibility = View.VISIBLE
            layoutVotedIndicator.visibility = View.VISIBLE

            // Setup results adapter
            if (resultsAdapter == null) {
                resultsAdapter = PollResultsAdapter(poll)
                rvPollResults.layoutManager = LinearLayoutManager(itemView.context)
                rvPollResults.isNestedScrollingEnabled = false
                rvPollResults.adapter = resultsAdapter
            } else {
                resultsAdapter?.updatePoll(poll)
            }
        }
    }
}
