package com.reflection.thecampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.data.model.SiteAnnouncement

class MyCoursesFragment : Fragment() {

    private val viewModel: MyCoursesViewModel by activityViewModels()
    private lateinit var adapter: CourseAdapter
    private var announcementView: View? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_courses, container, false)
        
        // Settings Navigation
        view.findViewById<android.view.View>(R.id.ivSettings).setOnClickListener {
            startActivity(android.content.Intent(activity, SettingsActivity::class.java))
        }

        // Referral Navigation
        view.findViewById<android.view.View>(R.id.ivGift).setOnClickListener {
            startActivity(android.content.Intent(activity, com.reflection.thecampus.ui.referral.ReferralActivity::class.java))
        }

        // Analytics Navigation
        view.findViewById<android.view.View>(R.id.ivAnalytics).setOnClickListener {
            startActivity(android.content.Intent(activity, AnalyticsActivity::class.java))
        }

        swipeRefresh = view.findViewById(R.id.swipeRefreshMyCourses)
        val rvMyCourses = view.findViewById<RecyclerView>(R.id.rvMyCourses)
        rvMyCourses.layoutManager = LinearLayoutManager(context)

        val shimmer = view.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerMyCourses)
        val emptyState = view.findViewById<View>(R.id.emptyState)

        // Setup swipe-to-refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // Observe refresh state
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.enrolledCourses.observe(viewLifecycleOwner) { courses ->
            timber.log.Timber.d("MyCoursesFragment: enrolledCourses observer triggered")
            timber.log.Timber.d("Received ${courses.size} enrolled courses")

            // Stop shimmer
            shimmer.stopShimmer()
            shimmer.visibility = android.view.View.GONE

            if (courses.isEmpty()) {
                timber.log.Timber.d("Showing empty state (no enrolled courses)")
                // Show empty state
                emptyState.visibility = View.VISIBLE
                rvMyCourses.visibility = View.GONE
                
                // Browse Courses button
                emptyState.findViewById<MaterialButton>(R.id.btnBrowseCourses).setOnClickListener {
                    // Navigate to Explore tab (index 1)
                    (activity as? MainActivity)?.navigateToTab(1)
                }
            } else {
                timber.log.Timber.d("Showing ${courses.size} courses in RecyclerView")
                courses.forEachIndexed { index, course ->
                    timber.log.Timber.d("  ${index + 1}. ${course.id}: ${course.basicInfo.name}")
                }

                // Show courses
                emptyState.visibility = View.GONE
                rvMyCourses.visibility = View.VISIBLE
                
                // All courses in this list are enrolled, so pass all their IDs
                val enrolledIds = courses.map { it.id }.toSet()
                
                // Only create adapter if it doesn't exist, otherwise update
                if (!::adapter.isInitialized) {
                    adapter = CourseAdapter(courses, enrolledIds) { course ->
                        val intent = android.content.Intent(activity, CourseDetailActivity::class.java)
                        intent.putExtra("COURSE_ID", course.id)
                        startActivity(intent)
                    }
                    rvMyCourses.adapter = adapter
                } else {
                    adapter.updateCourses(courses, enrolledIds)
                }
                timber.log.Timber.d("✓ Adapter set with ${courses.size} courses")
            }
        }
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        // Only load announcements when fragment is visible
        view?.let { loadAnnouncements(it) }
        // Start shimmer only when visible
        view?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerMyCourses)?.startShimmer()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop shimmer when not visible to save resources
        view?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerMyCourses)?.stopShimmer()
    }
    
    private fun loadAnnouncements(rootView: View) {
        val database = FirebaseDatabase.getInstance()
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val dismissedIds = prefs.getStringSet("dismissed_announcements", setOf()) ?: setOf()
        
        database.getReference("siteAnnouncements")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val announcements = mutableListOf<SiteAnnouncement>()
                    
                    for (child in snapshot.children) {
                        val announcement = child.getValue(SiteAnnouncement::class.java)
                        if (announcement != null) {
                            val announcementWithId = announcement.copy(id = child.key ?: "")
                            announcements.add(announcementWithId)
                        }
                    }
                    
                    // Filter active and non-dismissed announcements
                    val activeAnnouncement = announcements
                        .filter { it.status == "active" && !dismissedIds.contains(it.id) }
                        .maxByOrNull { it.createdAt }
                    
                    if (activeAnnouncement != null) {
                        showAnnouncement(rootView, activeAnnouncement)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Ignore errors
                }
            })
    }
    
    private fun showAnnouncement(rootView: View, announcement: SiteAnnouncement) {
        val stub = rootView.findViewById<ViewStub>(R.id.announcementStub)
        if (stub != null) {
            announcementView = stub.inflate()
        }
        
        announcementView?.let { view ->
            view.findViewById<TextView>(R.id.tvAnnouncementMessage).text = announcement.message
            
            val btnCta = view.findViewById<MaterialButton>(R.id.btnCta)
            if (announcement.ctaText.isNotEmpty() && announcement.ctaLink.isNotEmpty()) {
                btnCta.visibility = View.VISIBLE
                btnCta.text = announcement.ctaText
                btnCta.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announcement.ctaLink))
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            } else {
                btnCta.visibility = View.GONE
            }
            
            view.findViewById<ImageView>(R.id.ivClose).setOnClickListener {
                dismissAnnouncement(announcement.id)
                view.visibility = View.GONE
            }
        }
    }
    
    private fun dismissAnnouncement(announcementId: String) {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val dismissedIds = prefs.getStringSet("dismissed_announcements", setOf())?.toMutableSet() ?: mutableSetOf()
        dismissedIds.add(announcementId)
        prefs.edit().putStringSet("dismissed_announcements", dismissedIds).apply()
    }
}
