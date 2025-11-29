
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

class CourseAdapter(
    private val courses: List<Course>,
    private val enrolledCourseIds: Set<String> = emptySet(),
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tvCourseTag)
        val tvTitle: TextView = view.findViewById(R.id.tvCourseTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvCourseDescription)
        val ivThumbnail: android.widget.ImageView = view.findViewById(R.id.ivCourseThumbnail)
        val tvDiscountedPrice: TextView = view.findViewById(R.id.tvDiscountedPrice)
        val tvOriginalPrice: TextView = view.findViewById(R.id.tvOriginalPrice)
        val tvDiscountBadge: TextView = view.findViewById(R.id.tvDiscountBadge)
        val btnEnroll: MaterialButton = view.findViewById(R.id.btnEnroll)
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

            // Update button for enrolled courses - Green "Study Now" button
            holder.btnEnroll.text = "Study Now"
            holder.btnEnroll.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.colorSuccess)
            )
        } else {
            // Show pricing, hide progress
            holder.layoutPricing.visibility = View.VISIBLE
            holder.layoutProgress.visibility = View.GONE

            val originalPrice = course.pricing.price
            val discount = course.pricing.discount
            val discountedPrice = originalPrice - (originalPrice * discount / 100)

            holder.tvDiscountedPrice.text = "₹${discountedPrice.toInt()}"
            holder.tvOriginalPrice.text = "₹${originalPrice.toInt()}"
            holder.tvOriginalPrice.paintFlags = holder.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // Show discount badge if there's a discount
            if (discount > 0) {
                holder.tvDiscountBadge.visibility = View.VISIBLE
                holder.tvDiscountBadge.text = "${discount.toInt()}% OFF"
            } else {
                holder.tvDiscountBadge.visibility = View.GONE
            }

            // Update button for non-enrolled courses
            holder.btnEnroll.text = "+ Enroll Now"
            holder.btnEnroll.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.colorPrimary)
            )
        }

        holder.btnEnroll.setOnClickListener {
            onCourseClick(course)
        }
        
        holder.itemView.setOnClickListener {
            onCourseClick(course)
        }
    }

    override fun getItemCount() = courses.size
}
