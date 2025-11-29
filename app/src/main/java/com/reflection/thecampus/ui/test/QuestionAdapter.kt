package com.reflection.thecampus.ui.test

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Question

class QuestionAdapter(
    private val questions: List<Question>,
    private val onAnswerChanged: (String, String) -> Unit,
    private val getSavedAnswer: (String) -> String?,
    private val getSectionTitle: ((Int) -> String)? = null
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuestionIndex: TextView = itemView.findViewById(R.id.tvQuestionIndex)
        private val tvPositiveMarks: TextView = itemView.findViewById(R.id.tvPositiveMarks)
        private val tvNegativeMarks: TextView = itemView.findViewById(R.id.tvNegativeMarks)
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tvSectionTitle)
        private val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
        val cardQuestionImage: CardView = itemView.findViewById(R.id.cardQuestionImage)
        val ivQuestionImage: ImageView = itemView.findViewById(R.id.ivQuestionImage)
        private val optionsContainer: LinearLayout = itemView.findViewById(R.id.optionsContainer)

        fun bind(question: Question, position: Int) {
            tvQuestionIndex.text = "Question ${position + 1}"
            tvPositiveMarks.text = "+${question.marks}"
            tvNegativeMarks.text = "-${question.negativeMarks}"
            
            // Show section title if available
            getSectionTitle?.let { getTitle ->
                val sectionTitle = getTitle(position)
                if (sectionTitle.isNotEmpty()) {
                    tvSectionTitle.visibility = View.VISIBLE
                    tvSectionTitle.text = sectionTitle
                } else {
                    tvSectionTitle.visibility = View.GONE
                }
            } ?: run {
                tvSectionTitle.visibility = View.GONE
            }
            
            tvQuestionText.text = question.questionText

            // Image loading logic moved to onBindViewHolder for better control and Glide options
            // This block will be effectively overridden by onBindViewHolder's image handling
            // if (question.hasImage()) {
            //     cardQuestionImage.visibility = View.VISIBLE
            //     Glide.with(itemView.context)
            //         .load(question.questionImage)
            //         .into(ivQuestionImage)
            //         
            //     // Add click listener for zoom
            //     ivQuestionImage.setOnClickListener {
            //         showImageZoomDialog(question.questionImage)
            //     }
            // } else {
            //     cardQuestionImage.visibility = View.GONE
            // }

            optionsContainer.removeAllViews()
            val savedAnswer = getSavedAnswer(question.id)

            when (question.questionType) {
                "MCQ" -> setupMCQ(question, savedAnswer)
                "MSQ" -> setupMSQ(question, savedAnswer)
                "ShortAnswer" -> setupShortAnswer(question, savedAnswer)
                else -> setupMCQ(question, savedAnswer) // Default to MCQ
            }
        }
        


        private fun setupMCQ(question: Question, savedAnswer: String?) {
            question.options.forEach { (key, value) ->
                val optionCard = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_option_card, optionsContainer, false) as CardView
                    
                val radioButton = optionCard.findViewById<RadioButton>(R.id.rbOption)
                radioButton.text = "$key. $value"
                radioButton.tag = key
                // Do NOT change the ID, otherwise findViewById(R.id.rbOption) will fail later
                // radioButton.id = View.generateViewId()
                
                // Disable RadioButton's own click behavior to prevent conflicts
                radioButton.isClickable = false
                radioButton.isFocusable = false
                
                val isSelected = savedAnswer == key
                radioButton.isChecked = isSelected
                updateOptionCardStyle(optionCard, isSelected)
                
                // Only use card click listener for single-selection enforcement
                optionCard.setOnClickListener {
                    // Uncheck all radio buttons first
                    for (i in 0 until optionsContainer.childCount) {
                        val card = optionsContainer.getChildAt(i) as? CardView
                        val rb = card?.findViewById<RadioButton>(R.id.rbOption)
                        if (rb != null) {
                            rb.isChecked = false
                            updateOptionCardStyle(card, false)
                        }
                    }
                    
                    // Then check only this one
                    radioButton.isChecked = true
                    updateOptionCardStyle(optionCard, true)
                    onAnswerChanged(question.id, key)
                }
                
                optionsContainer.addView(optionCard)
            }
        }

        private fun setupMSQ(question: Question, savedAnswer: String?) {
            val selectedOptions = savedAnswer?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

            question.options.forEach { (key, value) ->
                val optionCard = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_option_card_msq, optionsContainer, false) as CardView
                    
                val checkBox = optionCard.findViewById<CheckBox>(R.id.cbOption)
                checkBox.text = "$key. $value"
                checkBox.tag = key
                
                val isSelected = selectedOptions.contains(key)
                checkBox.isChecked = isSelected
                updateOptionCardStyle(optionCard, isSelected)
                
                optionCard.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }
                
                checkBox.setOnCheckedChangeListener { _, _ ->
                    updateOptionCardStyle(optionCard, checkBox.isChecked)
                    
                    // Re-calculate full answer string
                    val currentSelected = mutableListOf<String>()
                    for (i in 0 until optionsContainer.childCount) {
                        val card = optionsContainer.getChildAt(i) as? CardView
                        val cb = card?.findViewById<CheckBox>(R.id.cbOption)
                        if (cb?.isChecked == true) {
                            currentSelected.add(cb.tag.toString())
                        }
                    }
                    onAnswerChanged(question.id, currentSelected.sorted().joinToString(","))
                }

                optionsContainer.addView(optionCard)
            }
        }

        private fun setupShortAnswer(question: Question, savedAnswer: String?) {
            val editText = EditText(itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                hint = "Enter your answer"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                setText(savedAnswer)
                background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_edit_text_rounded)
                setPadding(32, 32, 32, 32)
            }

            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    onAnswerChanged(question.id, s.toString())
                }
            })

            optionsContainer.addView(editText)
        }
        
        private fun updateOptionCardStyle(card: CardView?, isSelected: Boolean) {
            card?.let {
                if (isSelected) {
                    it.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.option_selected_bg))
//                    it.strokeColor = ContextCompat.getColor(itemView.context, R.color.option_selected_stroke)
//                    it.strokeWidth = 4
                } else {
                    val surfaceColor = com.google.android.material.color.MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorSurface, Color.WHITE)
                    it.setCardBackgroundColor(surfaceColor)
//                    it.strokeColor = ContextCompat.getColor(itemView.context, R.color.option_unselected_stroke)
//                    it.strokeWidth = 2
                }
            }
        }
        
        fun showImageZoomDialog(imageUrl: String) {
            val dialog = Dialog(itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_image_zoom)
            
            val ivZoomed = dialog.findViewById<ImageView>(R.id.ivZoomedImage)
            val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .fitCenter()
                .into(ivZoomed)
            
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_page, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        holder.bind(question, position)
        
        // Load image with high quality settings
        if (question.hasImage()) {
            holder.ivQuestionImage.visibility = View.VISIBLE
            holder.cardQuestionImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(question.questionImage)
                .override(1200, 1200) // Load larger size for better quality
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.ivQuestionImage)
                
            holder.cardQuestionImage.setOnClickListener {
                holder.showImageZoomDialog(question.questionImage)
            }
        } else {
            holder.ivQuestionImage.visibility = View.GONE
            holder.cardQuestionImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = questions.size
}
