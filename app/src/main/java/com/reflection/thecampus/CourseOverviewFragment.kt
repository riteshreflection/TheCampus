package com.reflection.thecampus

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
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
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var currentScrollPosition = 0

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
        
        // Setup stats card with conditional visibility
        setupStatsCard(view)
        
        // Setup period card
        setupPeriodCard(view)
        
        // Setup mentors RecyclerView - Horizontal Carousel
        val rvMentors = view.findViewById<RecyclerView>(R.id.rvMentors)
        rvMentors.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        // Fix scroll conflict with ViewPager - only block horizontal scrolls
        var initialX = 0f
        var initialY = 0f
        
        rvMentors.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = e.x
                        initialY = e.y
                        // Stop auto-scroll when user touches
                        stopAutoScroll()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = Math.abs(e.x - initialX)
                        val deltaY = Math.abs(e.y - initialY)
                        
                        // Only block parent if horizontal scroll is detected
                        if (deltaX > deltaY && deltaX > 10) {
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                        } else {
                            rv.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Resume auto-scroll when user releases
                        startAutoScroll(rvMentors)
                        rv.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        
        mentors?.let {
            if (it.isNotEmpty()) {
                val askMentorEnabled = course?.basicInfo?.askMentorEnabled ?: false
                rvMentors.adapter = MentorAdapter(it, isEnrolled, askMentorEnabled) { mentor ->
                    (activity as? CourseDetailActivity)?.openMentorChat(mentor)
                }
                
                // Start auto-scrolling
                startAutoScroll(rvMentors)
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
    
    private fun setupPeriodCard(view: View) {
        val course = course ?: return
        
        val cardPeriod = view.findViewById<CardView>(R.id.cardPeriod)
        val tvStartDate = view.findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = view.findViewById<TextView>(R.id.tvEndDate)
        
        // Check if dates are available
        if (course.schedule.startDate.isNotEmpty() && course.schedule.endDate.isNotEmpty()) {
            tvStartDate.text = formatDate(course.schedule.startDate)
            tvEndDate.text = formatDate(course.schedule.endDate)
            cardPeriod.visibility = View.VISIBLE
        } else {
            cardPeriod.visibility = View.GONE
        }
    }
    
    private fun formatDate(dateString: String): String {
        // Input format: "2025-12-24"
        // Output format: "24 Dec 2025"
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2]
                
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                
                "$day ${monthNames[month - 1]} $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun displayCourseInfo(view: View, course: Course) {
        // Course info
        view.findViewById<TextView>(R.id.tvCourseType).text = course.basicInfo.type
        view.findViewById<TextView>(R.id.tvCourseLevel).text = course.basicInfo.level
        view.findViewById<TextView>(R.id.tvCourseTitle).text = course.basicInfo.name
        view.findViewById<TextView>(R.id.tvDescription).text = course.basicInfo.description
        
        // Pricing is now handled by the fixed bottom banner in CourseDetailActivity
    }
    
    private fun startAutoScroll(recyclerView: RecyclerView) {
        stopAutoScroll() // Stop any existing auto-scroll
        
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val itemCount = recyclerView.adapter?.itemCount ?: 0
                
                if (itemCount > 0) {
                    currentScrollPosition = (currentScrollPosition + 1) % itemCount
                    recyclerView.smoothScrollToPosition(currentScrollPosition)
                }
                
                // Schedule next scroll after 3 seconds
                autoScrollHandler?.postDelayed(this, 3000)
            }
        }
        
        // Start auto-scrolling after 3 seconds
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, 3000)
    }
    
    private fun stopAutoScroll() {
        autoScrollRunnable?.let {
            autoScrollHandler?.removeCallbacks(it)
        }
        autoScrollHandler = null
        autoScrollRunnable = null
    }
    
    override fun onDestroyView() {
        stopAutoScroll()
        super.onDestroyView()
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
