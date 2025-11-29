package com.reflection.thecampus.ui.test

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Question

class PaletteAdapter(
    private val items: List<PaletteItem>,
    private val onQuestionClicked: (Int) -> Unit,
    private val getStatus: (String) -> QuestionStatus
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_QUESTION = 1
    }

    sealed class PaletteItem {
        data class Header(val title: String) : PaletteItem()
        data class QuestionItem(val question: Question, val index: Int) : PaletteItem()
    }

    enum class QuestionStatus {
        NOT_VISITED,
        NOT_ANSWERED,
        ANSWERED,
        MARKED_FOR_REVIEW,
        ANSWERED_AND_MARKED_FOR_REVIEW
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSectionHeader: TextView = itemView.findViewById(R.id.tvSectionHeader)
        fun bind(header: PaletteItem.Header) {
            tvSectionHeader.text = header.title
        }
    }

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPaletteItem: TextView = itemView.findViewById(R.id.tvPaletteItem)

        fun bind(item: PaletteItem.QuestionItem) {
            tvPaletteItem.text = "${item.index + 1}"
            
            val status = getStatus(item.question.id)
            when (status) {
                QuestionStatus.ANSWERED -> {
                    tvPaletteItem.setBackgroundResource(R.drawable.bg_palette_item_answered)
                    tvPaletteItem.setTextColor(Color.WHITE)
                }
                QuestionStatus.MARKED_FOR_REVIEW -> {
                    tvPaletteItem.setBackgroundResource(R.drawable.bg_palette_item_review)
                    tvPaletteItem.setTextColor(Color.WHITE)
                }
                QuestionStatus.ANSWERED_AND_MARKED_FOR_REVIEW -> {
                    tvPaletteItem.setBackgroundResource(R.drawable.bg_palette_item_answered_review)
                    tvPaletteItem.setTextColor(Color.WHITE)
                }
                QuestionStatus.NOT_ANSWERED, QuestionStatus.NOT_VISITED -> {
                    tvPaletteItem.setBackgroundResource(R.drawable.bg_palette_item_default)
                    tvPaletteItem.setTextColor(Color.BLACK)
                }
            }

            itemView.setOnClickListener {
                onQuestionClicked(item.index)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PaletteItem.Header -> VIEW_TYPE_HEADER
            is PaletteItem.QuestionItem -> VIEW_TYPE_QUESTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_palette_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_palette_question, parent, false)
            QuestionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PaletteItem.Header -> (holder as HeaderViewHolder).bind(item)
            is PaletteItem.QuestionItem -> (holder as QuestionViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
