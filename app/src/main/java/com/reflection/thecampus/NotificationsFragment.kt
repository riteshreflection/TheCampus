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
        checkNotificationPermission()
    }
    
    private fun checkNotificationPermission() {
        val context = context ?: return
        val card = view?.findViewById<View>(R.id.permissionAlertCard) ?: return
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                card.visibility = View.VISIBLE
                setupEnableButton()
            } else {
                card.visibility = View.GONE
            }
        } else {
            // For older Android versions, we rely on NotificationManager check
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                card.visibility = View.VISIBLE
                setupEnableButton()
            } else {
                card.visibility = View.GONE
            }
        }
    }

    private fun setupEnableButton() {
        view?.findViewById<View>(R.id.btnEnableNotifications)?.setOnClickListener {
            val intent = android.content.Intent().apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context?.packageName)
                } else {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", context?.packageName, null)
                }
            }
            startActivity(intent)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop shimmer when not visible
        view?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerNotifications)?.stopShimmer()
    }
}
