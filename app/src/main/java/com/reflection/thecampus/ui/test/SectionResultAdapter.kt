package com.reflection.thecampus.ui.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R

data class SectionResult(
    val title: String,
    val score: Double,
    val totalMarks: Double,
    val accuracy: Int
)

class SectionResultAdapter(private var sections: List<SectionResult>) : 
    RecyclerView.Adapter<SectionResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSectionTitle)
        val tvScore: TextView = view.findViewById(R.id.tvSectionScore)
        val pbAccuracy: ProgressBar = view.findViewById(R.id.pbSectionAccuracy)
        val tvAccuracy: TextView = view.findViewById(R.id.tvSectionAccuracy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = sections[position]
        
        holder.tvTitle.text = section.title
        holder.tvScore.text = "${String.format("%.1f", section.score)} / ${String.format("%.1f", section.totalMarks)}"
        
        holder.pbAccuracy.progress = section.accuracy
        holder.tvAccuracy.text = "${section.accuracy}%"
        
        // Color code accuracy
        val color = when {
            section.accuracy >= 80 -> holder.itemView.context.getColor(R.color.colorSuccess)
            section.accuracy >= 50 -> holder.itemView.context.getColor(R.color.colorPrimary)
            else -> holder.itemView.context.getColor(R.color.colorError)
        }
        // Note: Changing progress drawable color programmatically is tricky with LayerDrawable, 
        // keeping default primary color for now or could use setTint on the progress drawable.
    }

    override fun getItemCount() = sections.size
    
    fun updateData(newSections: List<SectionResult>) {
        sections = newSections
        notifyDataSetChanged()
    }
}
