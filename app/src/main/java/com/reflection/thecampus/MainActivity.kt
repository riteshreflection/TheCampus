package com.reflection.thecampus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set status bar color to match surface
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        
        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        // Initialize shared ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        tabLayout = findViewById(R.id.tab_layout)
        
        // Add tabs with icons only
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.book_open_svgrepo_com))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.book_bookmark_svgrepo_com))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.notification_bell_new_svgrepo_com))
        
        // Check Auth Status
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val loginPrompt = LoginPromptBottomSheet()
            loginPrompt.show(supportFragmentManager, LoginPromptBottomSheet.TAG)
        }

        // Set initial fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyCoursesFragment())
                .commit()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Soft haptic feedback
                tabLayout.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                
                val fragment = when (tab?.position) {
                    0 -> MyCoursesFragment()
                    1 -> DiscoverFragment()
                    2 -> NotificationsFragment()
                    else -> MyCoursesFragment()
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not needed
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not needed
            }
        })

        // Handle Back Press
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Exit App?")
            .setMessage("Are you sure you want to exit the application?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Check Session
        com.reflection.thecampus.utils.SessionManager.checkSession(this) { isValid ->
            if (!isValid) {
                // SessionManager handles logout and redirect
            } else {
                // Update FCM Token if session is valid
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    com.reflection.thecampus.utils.FCMManager.saveToken(this, currentUser.uid)
                }
            }
        }


        // Request Notification Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101 // Request Code
                )
            }
        }

        // Check App Status
        viewModel.appStatus.observe(this) { status ->
            when (status) {
                is AppStatus.Suspended -> {
                    startActivity(android.content.Intent(this, SuspendedActivity::class.java))
                    finish()
                }
                is AppStatus.Maintenance -> {
                    startActivity(android.content.Intent(this, MaintenanceActivity::class.java))
                    finish()
                }
                is AppStatus.ForceUpdate -> {
                    val existingFragment = supportFragmentManager.findFragmentByTag(UpdateAppBottomSheet.TAG)
                    if (existingFragment == null) {
                        val updateSheet = UpdateAppBottomSheet.newInstance(isForceUpdate = true)
                        updateSheet.show(supportFragmentManager, UpdateAppBottomSheet.TAG)
                    }
                }
                is AppStatus.Active -> {
                    // Dismiss update sheet if it was shown but not forced (future proofing) or if status changed back
                    val existingFragment = supportFragmentManager.findFragmentByTag(UpdateAppBottomSheet.TAG)
                    if (existingFragment != null && existingFragment is androidx.fragment.app.DialogFragment) {
                        // existingFragment.dismiss() // Optional: dismiss if status becomes active
                    }
                }
            }
        }
    }
    
    // Public method to navigate to specific tab
    fun navigateToTab(tabIndex: Int) {
        if (tabIndex in 0..2) {
            tabLayout.getTabAt(tabIndex)?.select()
        }
    }
}
