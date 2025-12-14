package com.reflection.thecampus.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Poll

class PollResultsAdapter(
    private var poll: Poll
) : RecyclerView.Adapter<PollResultsAdapter.ResultViewHolder>() {

    fun updatePoll(newPoll: Poll) {
        poll = newPoll
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(poll.options[position])
    }

    override fun getItemCount() = poll.options.size

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvResultOptionText: TextView = itemView.findViewById(R.id.tvResultOptionText)
        private val tvResultPercentage: TextView = itemView.findViewById(R.id.tvResultPercentage)
        private val progressBarResult: ProgressBar = itemView.findViewById(R.id.progressBarResult)

        fun bind(option: com.reflection.thecampus.data.model.PollOption) {
            tvResultOptionText.text = option.text
            
            val percentage = poll.getOptionPercentage(option.id)
            tvResultPercentage.text = "$percentage%"
            
            // Animate progress bar
            animateProgressBar(progressBarResult, percentage)
        }

        private fun animateProgressBar(progressBar: ProgressBar, targetPercentage: Int) {
            val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetPercentage)
            animator.duration = 800
            animator.interpolator = DecelerateInterpolator()
            animator.start()
        }
    }
}
