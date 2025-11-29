package com.reflection.thecampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class TestAdapter(
    private val tests: List<TestSummary>,
    private val isEnrolled: Boolean,
    private val onAttemptClick: (TestSummary) -> Unit
) : RecyclerView.Adapter<TestAdapter.TestViewHolder>() {

    class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
        val tvFreeTag: TextView = itemView.findViewById(R.id.tvFreeTag)
        val tvTestTitle: TextView = itemView.findViewById(R.id.tvTestTitle)
        val tvTestDescription: TextView = itemView.findViewById(R.id.tvTestDescription)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvTotalMarks: TextView = itemView.findViewById(R.id.tvTotalMarks)
        val btnAttempt: MaterialButton = itemView.findViewById(R.id.btnAttempt)
        val cardView: View = itemView // The root view is the CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test, parent, false)
        return TestViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]

        holder.tvSubject.text = test.subject
        holder.tvLevel.text = test.level
        holder.tvTestTitle.text = test.title
        holder.tvTestDescription.text = test.description
        holder.tvDuration.text = "${test.duration} Mins"
        holder.tvTotalMarks.text = "${test.totalMarks} Marks"

        if (test.isFree) {
            holder.tvFreeTag.visibility = View.VISIBLE
        } else {
            holder.tvFreeTag.visibility = View.GONE
        }

        // Locking Logic
        val isUnlocked = isEnrolled || test.isFree

        if (isUnlocked) {
            holder.cardView.alpha = 1.0f
            holder.btnAttempt.isEnabled = true
            holder.btnAttempt.text = "Attempt"
            holder.btnAttempt.setIconResource(R.drawable.ic_lock_open)
        } else {
            holder.cardView.alpha = 0.7f
            holder.btnAttempt.isEnabled = false
            holder.btnAttempt.text = "Locked"
            holder.btnAttempt.setIconResource(R.drawable.ic_lock_closed)
        }

        holder.btnAttempt.setOnClickListener {
            onAttemptClick(test)
        }
    }

    override fun getItemCount() = tests.size
}
