package com.reflection.thecampus

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.facebook.shimmer.ShimmerFrameLayout
import kotlin.math.floor

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: CourseDetailViewModel
    private var courseId: String = ""
    private var currentCourse: Course? = null
    private val mentors = mutableListOf<Faculty>()
    private var isEnrolled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_course_detail)
        
        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode
        
        // Apply window insets to root layout
        val rootLayout = findViewById<android.view.View>(android.R.id.content)
        com.reflection.thecampus.utils.WindowInsetsHelper.applySystemBarInsets(rootLayout)

        viewModel = ViewModelProvider(this)[CourseDetailViewModel::class.java]

        // Get course ID from intent (either from extra or deep link)
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        timber.log.Timber.d("CourseDetailActivity: courseId from intent extra: '$courseId'")

        // If not from extra, check if it's from a deep link
        if (courseId.isEmpty()) {
            intent.data?.let { uri ->
                timber.log.Timber.d("Checking deep link URI: $uri")
                // Extract course ID from path like /courses/-ObRDAUcpTMEcqoBBcJf
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 2 && pathSegments[0] == "courses") {
                    courseId = pathSegments[1]
                    timber.log.Timber.d("CourseId from deep link: $courseId")
                }
            }
        }

        timber.log.Timber.d("Final courseId: '$courseId'")

        if (courseId.isEmpty()) {
            timber.log.Timber.e("❌ CourseId is empty! Cannot load course")
            Toast.makeText(this, "Invalid course", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupAppBarScrollBehavior()
        setupViews()
        
        // Hide content initially
        findViewById<View>(R.id.appBarLayout).visibility = View.GONE
        findViewById<View>(R.id.viewPager).visibility = View.GONE
        
        // Start shimmer
        val shimmerViewContainer = findViewById<ShimmerFrameLayout>(R.id.shimmerViewContainer)
        shimmerViewContainer.visibility = View.VISIBLE
        shimmerViewContainer.startShimmer()
        
        loadCourseData()
    }

    private fun setupAppBarScrollBehavior() {
        // Status bar color remains consistent with background
        // No changes needed during scroll
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        val btnEnrollBanner = findViewById<MaterialButton>(R.id.btnEnrollBanner)
        
        btnEnrollBanner.setOnClickListener {
            // Check if user is logged in
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            
            if (currentUser == null) {
                // User not logged in - show login bottom sheet
                val loginSheet = LoginPromptBottomSheet()
                loginSheet.show(supportFragmentManager, LoginPromptBottomSheet.TAG)
            } else {
                // User logged in - proceed to checkout
                val intent = android.content.Intent(this, CheckoutActivity::class.java)
                intent.putExtra("COURSE_ID", courseId)
                startActivity(intent)
            }
        }
        
        // Force button to use custom background
        btnEnrollBanner.setBackgroundResource(R.drawable.bg_payment_button_premium)
        btnEnrollBanner.backgroundTintList = null
    }

    private fun loadCourseData() {
        timber.log.Timber.d("loadCourseData() - Loading course: $courseId")
        viewModel.loadCourse(courseId)

        viewModel.course.observe(this) { course ->
            timber.log.Timber.d("Course LiveData observer triggered")
            if (course != null) {
                timber.log.Timber.d("✓ Course data received: ${course.basicInfo.name}")
                currentCourse = course
                displayCourseData(course)
                loadMentors(course.instructorIds)
                
                // Stop shimmer and hide
                val shimmerViewContainer = findViewById<ShimmerFrameLayout>(R.id.shimmerViewContainer)
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.visibility = View.GONE
                
                // Show content
                findViewById<View>(R.id.appBarLayout).visibility = View.VISIBLE
                findViewById<View>(R.id.viewPager).visibility = View.VISIBLE
                
            } else {
                timber.log.Timber.w("⚠ Course data is null!")
                Toast.makeText(this, "Failed to load course", Toast.LENGTH_SHORT).show()
                findViewById<ShimmerFrameLayout>(R.id.shimmerViewContainer).stopShimmer()
                // Optionally keep content hidden or show error state
            }
        }

        viewModel.isEnrolled.observe(this) { enrolled ->
            timber.log.Timber.d("Enrollment status observer triggered: $enrolled")
            isEnrolled = enrolled
            updateEnrollmentUI(enrolled)
        }
    }

    private fun displayCourseData(course: Course) {
        // Setup collapsing toolbar
        val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        collapsingToolbar.title = course.basicInfo.name

        // Thumbnail
        val ivThumbnail = findViewById<ImageView>(R.id.ivCourseThumbnail)
        Glide.with(this)
            .load(course.pricing.thumbnailUrl)
            .placeholder(R.drawable.ic_book)
            .into(ivThumbnail)
        
        // Pricing - Update banner views
        val originalPrice = course.pricing.price
        val discountedPrice = course.pricing.discountedPrice

        // Use discountedPrice if available, otherwise use original price
        val finalPrice = if (discountedPrice > 0) discountedPrice else originalPrice

        findViewById<TextView>(R.id.tvBannerDiscountedPrice).text = "₹${finalPrice.toInt()}"
        
        // Show original price with strikethrough only if there's a discounted price
        val tvOriginalPrice = findViewById<TextView>(R.id.tvBannerOriginalPrice)
        val tvDiscount = findViewById<TextView>(R.id.tvBannerDiscount)
        
        if (discountedPrice > 0 && discountedPrice < originalPrice) {
            tvOriginalPrice.visibility = View.VISIBLE
            tvOriginalPrice.text = "₹${originalPrice.toInt()}"
            tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            
            // Calculate discount percentage from actual price difference
            val discountPercentage = ((originalPrice - discountedPrice) / originalPrice * 100).toInt()
            
            tvDiscount.text = "${discountPercentage}% OFF"
            tvDiscount.visibility = View.VISIBLE
        } else {
            tvOriginalPrice.visibility = View.GONE
            tvDiscount.visibility = View.GONE
        }
    }

    private fun loadMentors(instructorIds: List<String>) {
        if (instructorIds.isEmpty()) {
            setupTabs()
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("faculty")
        var loadedCount = 0

        for (instructorId in instructorIds) {
            database.child(instructorId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val faculty = snapshot.getValue(Faculty::class.java)
                    faculty?.let {
                        val facultyWithId = it.copy(id = snapshot.key ?: instructorId)
                        mentors.add(facultyWithId)
                    }
                    loadedCount++
                    if (loadedCount == instructorIds.size) {
                        setupTabs()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == instructorIds.size) {
                        setupTabs()
                    }
                }
            })
        }
    }

    private fun setupTabs() {
        val course = currentCourse ?: return
        
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = CourseTabsAdapter(this, course, mentors, isEnrolled)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Overview"
                1 -> {
                    tab.text = "Tests (${course.linkedTests.size})"
                    // Hide tab if no tests
                    if (course.linkedTests.isEmpty()) {
                        tab.view.visibility = View.GONE
                    }
                }
                2 -> tab.text = "Content"
                3 -> {
                    tab.text = "Classes (${course.linkedClasses.size})"
                    // Hide tab if no classes
                    if (course.linkedClasses.isEmpty()) {
                        tab.view.visibility = View.GONE
                    }
                }
            }
        }.attach()
    }

    private fun updateEnrollmentUI(isEnrolled: Boolean) {
        val cardPricingBanner = findViewById<CardView>(R.id.cardPricingBanner)

        // Hide pricing banner if user is already enrolled
        cardPricingBanner.visibility = if (isEnrolled) View.GONE else View.VISIBLE
        
        // Refresh tabs to update content
        currentCourse?.let { setupTabs() }
    }
    fun openMentorChat(mentor: Faculty) {
        if (!isEnrolled) {
            Toast.makeText(this, "Enroll in the course to chat with mentors", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = android.content.Intent(this, com.reflection.thecampus.ui.chat.ChatActivity::class.java)
        intent.putExtra("mentorId", mentor.id)
        intent.putExtra("mentorName", mentor.name)
        intent.putExtra("courseName", currentCourse?.basicInfo?.name ?: "Course")
        startActivity(intent)
    }
}
