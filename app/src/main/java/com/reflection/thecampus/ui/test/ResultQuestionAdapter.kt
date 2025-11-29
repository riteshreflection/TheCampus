package com.reflection.thecampus.ui.test

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Question

class ResultQuestionAdapter(
    private var questions: List<Question>,
    private var userAnswers: Map<String, Any>
) : RecyclerView.Adapter<ResultQuestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestionNumber: TextView = view.findViewById(R.id.tvQuestionNumber)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvMarks: TextView = view.findViewById(R.id.tvMarks)
        val tvQuestionText: TextView = view.findViewById(R.id.tvQuestionText)
        val ivQuestionImage: ImageView = view.findViewById(R.id.ivQuestionImage)
        val tvUserAnswer: TextView = view.findViewById(R.id.tvUserAnswer)
        val tvCorrectAnswer: TextView = view.findViewById(R.id.tvCorrectAnswer)
        val layoutExplanation: LinearLayout = view.findViewById(R.id.layoutExplanation)
        val tvExplanation: TextView = view.findViewById(R.id.tvExplanation)
        val ivExplanationImage: ImageView = view.findViewById(R.id.ivExplanationImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        val userAnswer = userAnswers[question.id]
        
        holder.tvQuestionNumber.text = "Q${position + 1}"
        holder.tvQuestionText.text = question.questionText
        
        // Image
        if (question.hasImage()) {
            holder.ivQuestionImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(question.questionImage)
                .into(holder.ivQuestionImage)
                
            holder.ivQuestionImage.setOnClickListener {
                showImageZoomDialog(holder.itemView.context, question.questionImage)
            }
        } else {
            holder.ivQuestionImage.visibility = View.GONE
        }

        // Logic
        val status: String
        val marks: Double
        val color: Int
        
        if (userAnswer == null || (userAnswer is String && userAnswer.isEmpty())) {
            status = "Unattempted"
            marks = 0.0
            color = Color.parseColor("#EF6C00") // Orange
        } else {
            val correct = checkAnswer(question, userAnswer)
            if (correct) {
                status = "Correct"
                marks = question.marks
                color = Color.parseColor("#4CAF50") // Green
            } else {
                status = "Incorrect"
                marks = -question.negativeMarks
                color = Color.parseColor("#F44336") // Red
            }
        }
        
        holder.tvStatus.text = status
        holder.tvStatus.setBackgroundColor(color)
        holder.tvMarks.text = if (marks > 0) "+$marks" else "$marks"
        holder.tvMarks.setTextColor(color)

        // Display Answers
        holder.tvUserAnswer.text = formatAnswer(userAnswer)
        holder.tvCorrectAnswer.text = formatCorrectAnswer(question)

        // Explanation
        if (question.hasExplanation()) {
            holder.layoutExplanation.visibility = View.VISIBLE
            holder.tvExplanation.text = question.explanation?.text
            if (!question.explanation?.imageUrl.isNullOrEmpty()) {
                holder.ivExplanationImage.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(question.explanation?.imageUrl)
                    .into(holder.ivExplanationImage)
                    
                holder.ivExplanationImage.setOnClickListener {
                    showImageZoomDialog(holder.itemView.context, question.explanation!!.imageUrl)
                }
            } else {
                holder.ivExplanationImage.visibility = View.GONE
            }
        } else {
            holder.layoutExplanation.visibility = View.GONE
        }
    }

    private fun checkAnswer(question: Question, userAnswer: Any): Boolean {
        return try {
            when {
                question.isMCQ() -> {
                    // Expect List<String> or String
                    val userList = if (userAnswer is List<*>) userAnswer as List<String> else listOf(userAnswer.toString())
                    val correctList = question.correctAnswers
                    userList == correctList
                }
                question.isMSQ() -> {
                    // Expect List<String>
                    val userList = if (userAnswer is List<*>) userAnswer as List<String> else listOf(userAnswer.toString())
                    val correctList = question.correctAnswers
                    userList.sorted() == correctList.sorted()
                }
                question.isShortAnswer() -> {
                    // Expect Number or String
                    val userVal = userAnswer.toString().toDoubleOrNull() ?: return false
                    val range = question.correctNumericalAnswerRange ?: return false
                    userVal >= range.from && userVal <= range.to
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun formatAnswer(answer: Any?): String {
        if (answer == null) return "Not Answered"
        return when (answer) {
            is List<*> -> answer.joinToString(", ")
            else -> answer.toString()
        }
    }

    private fun formatCorrectAnswer(question: Question): String {
        return when {
            question.isShortAnswer() -> "${question.correctNumericalAnswerRange?.from} - ${question.correctNumericalAnswerRange?.to}"
            else -> question.correctAnswers.joinToString(", ")
        }
    }
    
    private fun showImageZoomDialog(context: android.content.Context, imageUrl: String) {
        val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_image_zoom)
        
        val ivZoomed = dialog.findViewById<ImageView>(R.id.ivZoomedImage)
        val btnClose = dialog.findViewById<android.widget.ImageButton>(R.id.btnClose)
        
        Glide.with(context)
            .load(imageUrl)
            .fitCenter()
            .into(ivZoomed)
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun getItemCount() = questions.size
    
    fun updateData(newQuestions: List<Question>, newAnswers: Map<String, Any>) {
        questions = newQuestions
        userAnswers = newAnswers
        notifyDataSetChanged()
    }
}
