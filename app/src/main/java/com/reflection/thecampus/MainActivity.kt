package com.reflection.thecampus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var loginBanner: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dynamic theme BEFORE super.onCreate to ensure it takes effect for all attributes
        com.reflection.thecampus.utils.ThemeHelper.applyTheme(this)
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

        // Initialize ViewPager2 and TabLayout
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        
        // Apply primary color (black or white) to tab indicator
        val primaryColor = com.reflection.thecampus.utils.DynamicThemeUtils.getPrimaryColor(this)
        tabLayout.setSelectedTabIndicatorColor(primaryColor)
        
        // Create ColorStateList for tab icons
        val iconColorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(
                primaryColor,
                getColor(R.color.colorTextSecondary)
            )
        )
        tabLayout.tabIconTint = iconColorStateList
        
        // Set up ViewPager2 with adapter
        val adapter = MainFragmentAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false // Disable swipe navigation
        viewPager.offscreenPageLimit = 3 // Keep all 4 fragments in memory
        
        // Connect TabLayout with ViewPager2
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Set tab icons
            tab.icon = when (position) {
                0 -> AppCompatResources.getDrawable(this,R.drawable.book_open_svgrepo_com)
                1 -> AppCompatResources.getDrawable(this,R.drawable.book_bookmark_svgrepo_com)
                2 -> AppCompatResources.getDrawable(this,R.drawable.ic_chat_bubble) // Chat icon
                3 -> AppCompatResources.getDrawable(this,R.drawable.notification_bell_new_svgrepo_com)
                else -> null
            }
            // Add Beta badge to Chat tab
            if (position == 2) {
                tab.orCreateBadge.apply {
                    text = "Beta"
                    backgroundColor = getColor(R.color.orange_beta)
                }
            }
        }
        tabLayoutMediator.attach()
        
        // Add haptic feedback on tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabLayout.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Initialize Premium Login Banner
        setupLoginBanner()

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
    
    private fun setupLoginBanner() {
        loginBanner = findViewById(R.id.loginPromptBanner)
        val sparkle1 = loginBanner.findViewById<android.widget.ImageView>(R.id.sparkle1)
        val sparkle2 = loginBanner.findViewById<android.widget.ImageView>(R.id.sparkle2)
        val sparkle3 = loginBanner.findViewById<android.widget.ImageView>(R.id.sparkle3)
        val btnLogin = loginBanner.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLoginPrompt)
        
        // Start shimmer animations with different delays for natural effect
        val shimmerAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shimmer_shine)
        sparkle1.startAnimation(shimmerAnim)
        
        sparkle2.postDelayed({
            sparkle2.startAnimation(shimmerAnim)
        }, 500)
        
        sparkle3.postDelayed({
            sparkle3.startAnimation(shimmerAnim)
        }, 1000)
        
        // Magical click effect for login button
        btnLogin.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val pressAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.magical_press)
                    view.startAnimation(pressAnim)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val releaseAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.magical_release)
                    view.startAnimation(releaseAnim)
                }
            }
            false // Return false to allow click event to process
        }
        
        // Login button click
        btnLogin.setOnClickListener {
            val intent = android.content.Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
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
        
        // Show/hide premium login banner based on authentication
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        loginBanner.visibility = if (authUser == null) android.view.View.VISIBLE else android.view.View.GONE


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
        if (tabIndex in 0..3) {
            viewPager.currentItem = tabIndex
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Detach TabLayoutMediator to prevent memory leaks
        tabLayoutMediator.detach()
    }
}
