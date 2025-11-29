package com.reflection.thecampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MentorAdapter(
    private val mentors: List<Faculty>,
    private val isEnrolled: Boolean,
    private val onAskMentorClick: (Faculty) -> Unit
) : RecyclerView.Adapter<MentorAdapter.MentorViewHolder>() {

    class MentorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMentorPhoto: ImageView = view.findViewById(R.id.ivMentorPhoto)
        val tvMentorName: TextView = view.findViewById(R.id.tvMentorName)
        val tvMentorSpecs: TextView = view.findViewById(R.id.tvMentorSpecs)
        val tvMentorExperience: TextView = view.findViewById(R.id.tvMentorExperience)
        val btnAskMentor: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnAskMentor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mentor, parent, false)
        return MentorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MentorViewHolder, position: Int) {
        val mentor = mentors[position]
        val context = holder.itemView.context

        holder.tvMentorName.text = mentor.name
        holder.tvMentorSpecs.text = mentor.specifications
        holder.tvMentorExperience.text = mentor.experience

        // Load profile picture
        if (mentor.profilePictureUrl.isNotEmpty()) {
            Glide.with(context)
                .load(mentor.profilePictureUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(holder.ivMentorPhoto)
        } else {
            holder.ivMentorPhoto.setImageResource(R.drawable.ic_person)
        }
        
        // Handle Ask Mentor button
        holder.btnAskMentor.isEnabled = isEnrolled
        holder.btnAskMentor.alpha = if (isEnrolled) 1.0f else 0.5f
        holder.btnAskMentor.setOnClickListener {
            onAskMentorClick(mentor)
        }
    }

    override fun getItemCount() = mentors.size
}
