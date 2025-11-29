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

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: CourseDetailViewModel
    private var courseId: String = ""
    private var currentCourse: Course? = null
    private val mentors = mutableListOf<Faculty>()
    private var isEnrolled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

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
        val btnEnroll = findViewById<MaterialButton>(R.id.btnEnroll)
        
        btnEnroll.setOnClickListener {
            // Navigate to CheckoutActivity
            val intent = android.content.Intent(this, CheckoutActivity::class.java)
            intent.putExtra("COURSE_ID", courseId)
            startActivity(intent)
        }
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
        
        // Pricing
        val originalPrice = course.pricing.price
        val discount = course.pricing.discount
        val discountedPrice = originalPrice - (originalPrice * discount / 100)

        findViewById<TextView>(R.id.tvDiscountedPrice).text = "₹${discountedPrice.toInt()}"
        findViewById<TextView>(R.id.tvOriginalPrice).text = "₹${originalPrice.toInt()}"
        findViewById<TextView>(R.id.tvOriginalPrice).paintFlags = 
            findViewById<TextView>(R.id.tvOriginalPrice).paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        if (discount > 0) {
            findViewById<TextView>(R.id.tvDiscount).text = "${discount}% OFF"
            findViewById<TextView>(R.id.tvDiscount).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.tvDiscount).visibility = View.GONE
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
        val cardPricing = findViewById<CardView>(R.id.cardPricing)

        // Hide pricing card if user is already enrolled
        cardPricing.visibility = if (isEnrolled) View.GONE else View.VISIBLE
        
        // Refresh tabs to update pricing visibility in Overview
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
