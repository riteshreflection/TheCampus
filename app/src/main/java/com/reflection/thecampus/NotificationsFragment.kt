package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class NotificationsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: AnnouncementAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        swipeRefresh = view.findViewById(R.id.swipeRefreshNotifications)
        val rvAnnouncements = view.findViewById<RecyclerView>(R.id.rvAnnouncements)
        val shimmer = view.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerNotifications)
        val layoutEmpty = view.findViewById<View>(R.id.layoutEmpty)

        rvAnnouncements.layoutManager = LinearLayoutManager(context)

        // Setup swipe-to-refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }

        // Observe refresh state
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.announcements.observe(viewLifecycleOwner) { announcements ->
            // Stop shimmer
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE

            if (announcements.isEmpty()) {
                // Show empty state
                layoutEmpty.visibility = View.VISIBLE
                rvAnnouncements.visibility = View.GONE
            } else {
                // Show announcements
                layoutEmpty.visibility = View.GONE
                rvAnnouncements.visibility = View.VISIBLE
                
                // Only create adapter if it doesn't exist, otherwise update
                if (!::adapter.isInitialized) {
                    adapter = AnnouncementAdapter(announcements)
                    rvAnnouncements.adapter = adapter
                } else {
                    adapter.updateAnnouncements(announcements)
                }
            }
        }

        return view
    }
    
    override fun onResume() {
        super.onResume()
        // Start shimmer only when visible
        view?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerNotifications)?.startShimmer()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop shimmer when not visible
        view?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerNotifications)?.stopShimmer()
    }
}
