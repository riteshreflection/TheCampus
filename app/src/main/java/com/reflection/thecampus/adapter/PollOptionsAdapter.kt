package com.reflection.thecampus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.PollOption

class PollOptionsAdapter(
    private var options: List<PollOption>,
    private var pollType: String
) : RecyclerView.Adapter<PollOptionsAdapter.OptionViewHolder>() {

    private val selectedOptionIds = mutableSetOf<String>()

    fun updateOptions(newOptions: List<PollOption>, newPollType: String) {
        options = newOptions
        pollType = newPollType
        selectedOptionIds.clear()
        notifyDataSetChanged()
    }

    fun getSelectedOptions(): List<String> {
        return selectedOptionIds.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBarOption: ProgressBar = itemView.findViewById(R.id.progressBarOption)
        private val tvOptionText: TextView = itemView.findViewById(R.id.tvOptionText)
        private val tvOptionPercentage: TextView = itemView.findViewById(R.id.tvOptionPercentage)

        fun bind(option: PollOption) {
            tvOptionText.text = option.text
            
            val isSelected = selectedOptionIds.contains(option.id)
            
            // Show selection with progress bar fill (0% or 100%)
            progressBarOption.progress = if (isSelected) 100 else 0
            
            // Show percentage when selected
            if (isSelected) {
                tvOptionPercentage.visibility = View.VISIBLE
                tvOptionPercentage.text = "100%"
            } else {
                tvOptionPercentage.visibility = View.GONE
            }
            
            itemView.setOnClickListener {
                when (pollType) {
                    "single", "quiz" -> {
                        // Single selection - clear others and select this
                        selectedOptionIds.clear()
                        selectedOptionIds.add(option.id)
                        notifyDataSetChanged()
                    }
                    "multiple" -> {
                        // Multiple selection - toggle this option
                        if (selectedOptionIds.contains(option.id)) {
                            selectedOptionIds.remove(option.id)
                        } else {
                            selectedOptionIds.add(option.id)
                        }
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
        }
    }
}
