package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class DiscoverFragment : Fragment() {

    private val viewModel: DiscoverViewModel by activityViewModels()
    private lateinit var adapter: CourseAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private var currentCourses: List<Course> = emptyList()
    private var enrolledCourseIds: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        swipeRefresh = view.findViewById(R.id.swipeRefreshDiscover)
        val rvFeaturedCourses = view.findViewById<RecyclerView>(R.id.rvFeaturedCourses)
        rvFeaturedCourses.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rvFeaturedCourses.isNestedScrollingEnabled = false

        val shimmer = view.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerDiscover)
        shimmer.startShimmer()

        // Setup swipe-to-refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // Observe refresh state
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefresh.isRefreshing = isRefreshing
        }

        // Observe courses
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            timber.log.Timber.d("DiscoverFragment: courses observer triggered")
            timber.log.Timber.d("Received ${courses.size} courses")

            // Stop shimmer and show content
            shimmer.stopShimmer()
            shimmer.visibility = android.view.View.GONE
            rvFeaturedCourses.visibility = android.view.View.VISIBLE

            currentCourses = courses

            if (courses.isEmpty()) {
                timber.log.Timber.w("⚠ No courses to display in Discover")
            } else {
                timber.log.Timber.d("Updating adapter with ${courses.size} courses")
                courses.take(5).forEachIndexed { index, course ->
                    timber.log.Timber.d("  ${index + 1}. ${course.id}: ${course.basicInfo.name}")
                }
            }

            updateAdapter(rvFeaturedCourses)
        }

        // Observe enrolled course IDs
        viewModel.enrolledCourseIds.observe(viewLifecycleOwner) { ids ->
            timber.log.Timber.d("DiscoverFragment: enrolledCourseIds observer triggered")
            timber.log.Timber.d("Received ${ids.size} enrolled course IDs")
            enrolledCourseIds = ids
            updateAdapter(rvFeaturedCourses)
        }

        return view
    }

    private fun updateAdapter(recyclerView: RecyclerView) {
        timber.log.Timber.d("updateAdapter called - courses: ${currentCourses.size}, enrolled IDs: ${enrolledCourseIds.size}")

        if (currentCourses.isNotEmpty()) {
            adapter = CourseAdapter(currentCourses, enrolledCourseIds) { course ->
                timber.log.Timber.d("Course clicked: ${course.id} - ${course.basicInfo.name}")
                val intent = Intent(activity, CourseDetailActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
            timber.log.Timber.d("✓ Adapter updated successfully")
        } else {
            timber.log.Timber.w("⚠ Cannot update adapter - no courses available")
        }
    }
}
