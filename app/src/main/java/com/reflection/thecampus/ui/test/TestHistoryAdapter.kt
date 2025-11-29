package com.reflection.thecampus.ui.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.TestAttempt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestHistoryAdapter(
    private var attempts: List<TestAttempt>,
    private val onAttemptClicked: (TestAttempt) -> Unit
) : RecyclerView.Adapter<TestHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTestTitle: TextView = view.findViewById(R.id.tvTestTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attempt = attempts[position]
        
        holder.tvTestTitle.text = if (attempt.testTitle.isNotEmpty()) attempt.testTitle else "Test ID: ${attempt.testId}"
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(attempt.submittedAt))
        
        holder.tvScore.text = "Score: ${String.format("%.2f", attempt.score)}"
        
        holder.itemView.setOnClickListener {
            onAttemptClicked(attempt)
        }
    }

    override fun getItemCount() = attempts.size
    
    fun updateData(newAttempts: List<TestAttempt>) {
        attempts = newAttempts
        notifyDataSetChanged()
    }
}
