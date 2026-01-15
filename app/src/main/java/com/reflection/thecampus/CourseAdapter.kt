package com.reflection.thecampus

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlin.math.floor

class CourseAdapter(
    private var courses: List<Course>,
    private var enrolledCourseIds: Set<String> = emptySet(),
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {
    
    fun updateCourses(newCourses: List<Course>, newEnrolledIds: Set<String>) {
        courses = newCourses
        enrolledCourseIds = newEnrolledIds
        notifyDataSetChanged()
    }

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tvCourseTag)
        val tvTitle: TextView = view.findViewById(R.id.tvCourseTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvCourseDescription)
        val ivThumbnail: android.widget.ImageView = view.findViewById(R.id.ivCourseThumbnail)
        val tvDiscountedPrice: TextView = view.findViewById(R.id.tvDiscountedPrice)
        val tvOriginalPrice: TextView = view.findViewById(R.id.tvOriginalPrice)
        val tvDiscountBadge: TextView = view.findViewById(R.id.tvDiscountBadge)
        val btnEnroll: MaterialButton = view.findViewById(R.id.btnEnroll)
        val btnStudyNow: MaterialButton = view.findViewById(R.id.btnStudyNow)
        val layoutPricing: LinearLayout = view.findViewById(R.id.layoutPricing)
        val layoutProgress: LinearLayout = view.findViewById(R.id.layoutProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvProgressPercentage: TextView = view.findViewById(R.id.tvProgressPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        val context = holder.itemView.context
        val isEnrolled = enrolledCourseIds.contains(course.id)
        
        holder.tvTag.text = course.basicInfo.type.ifEmpty { "Course" }
        holder.tvTitle.text = course.basicInfo.name
        holder.tvDescription.text = course.basicInfo.description

        // Load thumbnail
        com.bumptech.glide.Glide.with(context)
            .load(course.pricing.thumbnailUrl)
            .placeholder(R.drawable.ic_book)
            .into(holder.ivThumbnail)

        if (isEnrolled) {
            // Hide pricing and discount badge, show progress
            holder.layoutPricing.visibility = View.GONE
            holder.layoutProgress.visibility = View.VISIBLE
            holder.tvDiscountBadge.visibility = View.GONE

            // Calculate progress based on schedule totals vs linked items
            val totalPlanned = course.schedule.totalLectures + course.schedule.totalTests
            val totalLinked = course.linkedTests.size + course.linkedClasses.size
            
            // Progress is based on how much content is available vs planned
            val progress = if (totalPlanned > 0) {
                ((totalLinked.toFloat() / totalPlanned.toFloat()) * 100).toInt()
            } else {
                0
            }

            holder.progressBar.progress = progress
            holder.tvProgressPercentage.text = "$progress%"

            // Show Study Now button, hide Enroll button
            holder.btnStudyNow.visibility = View.VISIBLE
            holder.btnEnroll.visibility = View.GONE
        } else {
            // Show pricing, hide progress
            holder.layoutPricing.visibility = View.VISIBLE
            holder.layoutProgress.visibility = View.GONE

            val originalPrice = course.pricing.price
            val discountedPrice = course.pricing.discountedPrice

            // Use discountedPrice if available, otherwise use original price
            val finalPrice = if (discountedPrice > 0) discountedPrice else originalPrice

            holder.tvDiscountedPrice.text = "₹${finalPrice.toInt()}"
            
            // Show original price with strikethrough only if there's a discounted price
            if (discountedPrice > 0 && discountedPrice < originalPrice) {
                holder.tvOriginalPrice.visibility = View.VISIBLE
                holder.tvOriginalPrice.text = "₹${originalPrice.toInt()}"
                holder.tvOriginalPrice.paintFlags = holder.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                
                // Calculate discount percentage from actual price difference
                val discountPercentage = ((originalPrice - discountedPrice) / originalPrice * 100).toInt()
                
                // Show discount badge
                holder.tvDiscountBadge.visibility = View.VISIBLE
                holder.tvDiscountBadge.text = "${discountPercentage}% OFF"
            } else {
                holder.tvOriginalPrice.visibility = View.GONE
                holder.tvDiscountBadge.visibility = View.GONE
            }

            // Show Enroll button, hide Study Now button
            holder.btnEnroll.visibility = View.VISIBLE
            holder.btnStudyNow.visibility = View.GONE

            // Update button for non-enrolled courses
            holder.btnEnroll.text = "+ Enroll Now"
            
            // Get theme attribute color for button background
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            holder.btnEnroll.backgroundTintList = ColorStateList.valueOf(typedValue.data)
            
            // Use colorOnPrimary for text color (theme-aware)
            val textColorTypedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, textColorTypedValue, true)
            holder.btnEnroll.setTextColor(textColorTypedValue.data)
        }

        // Set click listeners for both buttons
        holder.btnEnroll.setOnClickListener {
            onCourseClick(course)
        }
        
        holder.btnStudyNow.setOnClickListener {
            onCourseClick(course)
        }
        
        holder.itemView.setOnClickListener {
            onCourseClick(course)
        }
    }

    override fun getItemCount() = courses.size
}
