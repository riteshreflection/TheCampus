package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CourseOverviewFragment : Fragment() {

    private var course: Course? = null
    private var mentors: ArrayList<Faculty>? = null
    private var isEnrolled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            course = it.getParcelable(ARG_COURSE)
            mentors = it.getParcelableArrayList(ARG_MENTORS)
            isEnrolled = it.getBoolean(ARG_IS_ENROLLED, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        
        course?.let { displayCourseInfo(view, it) }
        
        // Hide pricing card if enrolled
        val cardPricing = view.findViewById<CardView>(R.id.cardPricing)
        cardPricing.visibility = if (isEnrolled) View.GONE else View.VISIBLE
        
        // Setup stats card with conditional visibility
        setupStatsCard(view)
        
        // Setup mentors RecyclerView
        val rvMentors = view.findViewById<RecyclerView>(R.id.rvMentors)
        rvMentors.layoutManager = LinearLayoutManager(context)
        mentors?.let {
            if (it.isNotEmpty()) {
                rvMentors.adapter = MentorAdapter(it, isEnrolled) { mentor ->
                    (activity as? CourseDetailActivity)?.openMentorChat(mentor)
                }
            }
        }
        
        return view
    }

    private fun setupStatsCard(view: View) {
        val course = course ?: return
        
        val cardStats = view.findViewById<CardView>(R.id.cardStats)
        val layoutLectures = view.findViewById<View>(R.id.layoutLectures)
        val layoutTests = view.findViewById<View>(R.id.layoutTests)
        val tvStatsLectures = view.findViewById<TextView>(R.id.tvStatsLectures)
        val tvStatsTests = view.findViewById<TextView>(R.id.tvStatsTests)
        
        // Set values
        tvStatsLectures.text = course.schedule.totalLectures.toString()
        tvStatsTests.text = course.schedule.totalTests.toString()
        
        // Hide individual sections if 0
        layoutLectures.visibility = if (course.schedule.totalLectures > 0) View.VISIBLE else View.GONE
        layoutTests.visibility = if (course.schedule.totalTests > 0) View.VISIBLE else View.GONE
        
        // Hide entire card if both are 0
        if (course.schedule.totalLectures == 0 && course.schedule.totalTests == 0) {
            cardStats.visibility = View.GONE
        }
    }

    private fun displayCourseInfo(view: View, course: Course) {
        // Course info
        view.findViewById<TextView>(R.id.tvCourseType).text = course.basicInfo.type
        view.findViewById<TextView>(R.id.tvCourseLevel).text = course.basicInfo.level
        view.findViewById<TextView>(R.id.tvCourseTitle).text = course.basicInfo.name
        view.findViewById<TextView>(R.id.tvDescription).text = course.basicInfo.description
        
        // Pricing
        val originalPrice = course.pricing.price
        val discount = course.pricing.discount
        val discountedPrice = originalPrice - (originalPrice * discount / 100)

        view.findViewById<TextView>(R.id.tvDiscountedPrice).text = "₹${discountedPrice.toInt()}"
        view.findViewById<TextView>(R.id.tvOriginalPrice).text = "₹${originalPrice.toInt()}"
        view.findViewById<TextView>(R.id.tvOriginalPrice).paintFlags = 
            view.findViewById<TextView>(R.id.tvOriginalPrice).paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        if (discount > 0) {
            view.findViewById<TextView>(R.id.tvDiscount).text = "${discount}% OFF"
            view.findViewById<TextView>(R.id.tvDiscount).visibility = View.VISIBLE
        } else {
            view.findViewById<TextView>(R.id.tvDiscount).visibility = View.GONE
        }
    }

    companion object {
        private const val ARG_COURSE = "course"
        private const val ARG_MENTORS = "mentors"
        private const val ARG_IS_ENROLLED = "is_enrolled"

        fun newInstance(course: Course, mentors: List<Faculty>, isEnrolled: Boolean) =
            CourseOverviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COURSE, course)
                    putParcelableArrayList(ARG_MENTORS, ArrayList(mentors))
                    putBoolean(ARG_IS_ENROLLED, isEnrolled)
                }
            }
    }
}
