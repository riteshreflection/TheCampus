package com.reflection.thecampus.ui.course

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.reflection.thecampus.CheckoutActivity
import com.reflection.thecampus.Course
import com.reflection.thecampus.CourseContentItem
import com.reflection.thecampus.R
import kotlin.math.floor

class CourseContentFragment : Fragment() {

    private var course: Course? = null
    private var isEnrolled: Boolean = false
    private lateinit var adapter: CourseContentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            course = it.getParcelable(ARG_COURSE)
            isEnrolled = it.getBoolean(ARG_IS_ENROLLED, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)
        updateList()
    }

    private fun setupRecyclerView(view: View) {
        val rvContent = view.findViewById<RecyclerView>(R.id.rvContent)
        rvContent.layoutManager = LinearLayoutManager(context)
        
        adapter = CourseContentAdapter(
            isEnrolled = isEnrolled,
            onFolderClick = { item ->
                // Show enrollment bottom sheet for locked items
                if (!isEnrolled && !item.isPublic && !item.isFree) {
                    showEnrollmentBottomSheet()
                }
            },
            onFileClick = { item ->
                openFile(item)
            }
        )
        rvContent.adapter = adapter
    }

    private fun openFile(item: CourseContentItem) {
        // If user is enrolled, allow direct access
        if (isEnrolled) {
            openFileUrl(item.url)
            return
        }
        
        // For locked content, show enrollment prompt
        if (!item.isPublic && !item.isFree) {
            showEnrollmentBottomSheet()
            return
        }
        
        // For free/public content, validate profile completion
        com.reflection.thecampus.utils.ProfileValidator.validateProfileForFreeAccess(
            requireActivity()
        ) {
            openFileUrl(item.url)
        }
    }
    
    private fun openFileUrl(url: String) {
        if (url.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Invalid file URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateList() {
        val contentMap = course?.content ?: emptyMap()
        val view = view ?: return
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        if (contentMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            adapter.submitList(emptyList())
            return
        }
        tvEmpty.visibility = View.GONE

        val tree = buildTree(contentMap)
        adapter.submitList(tree)
    }

    private fun buildTree(contentMap: Map<String, CourseContentItem>): List<ContentNode> {
        // 1. Find all root items
        val rootItems = contentMap.values
            .filter { it.parentId.isNullOrEmpty() && it.status == "published" }
            .sortedBy { it.name }

        // 2. Recursively build nodes
        return rootItems.map { item ->
            buildNode(item, contentMap)
        }
    }

    private fun buildNode(item: CourseContentItem, contentMap: Map<String, CourseContentItem>): ContentNode {
        // Find children of this item
        val childrenItems = contentMap.values
            .filter { it.parentId == item.id && it.status == "published" }
            .sortedBy { it.name }
        
        // Check if this item or any parent is free/public
        val isInheritedFree = isParentFreeOrPublic(item, contentMap)
        
        // Create modified item with inherited free status
        val modifiedItem = if (isInheritedFree && !item.isPublic && !item.isFree) {
            item.copy(isPublic = true) // Inherit parent's free status
        } else {
            item
        }
        
        val childrenNodes = childrenItems.map { child ->
            buildNode(child, contentMap)
        }

        return ContentNode(modifiedItem, childrenNodes)
    }
    
    private fun isParentFreeOrPublic(item: CourseContentItem, contentMap: Map<String, CourseContentItem>): Boolean {
        var currentParentId = item.parentId
        while (!currentParentId.isNullOrEmpty()) {
            val parent = contentMap[currentParentId]
            if (parent != null && (parent.isPublic || parent.isFree)) {
                return true
            }
            currentParentId = parent?.parentId
        }
        return false
    }

    private fun showEnrollmentBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_enroll_prompt, null)
        bottomSheetDialog.setContentView(view)
        
        // Start sparkle animations for all particles
        val sparkleAnim = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.sparkle_glitter)
        
        // Main sparkles
        view.findViewById<android.widget.ImageView>(R.id.sparkle1)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.sparkle2)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.sparkle3)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.sparkle4)?.startAnimation(sparkleAnim)
        
        // Fine particles
        view.findViewById<android.widget.ImageView>(R.id.particle1)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle2)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle3)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle4)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle5)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle6)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle7)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle8)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle9)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle10)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle11)?.startAnimation(sparkleAnim)
        view.findViewById<android.widget.ImageView>(R.id.particle12)?.startAnimation(sparkleAnim)
        
        // Set course name
        val tvCourseName = view.findViewById<TextView>(R.id.tvCourseName)
        tvCourseName.text = course?.basicInfo?.name ?: "This Course"
        
        // Set pricing with discount (matching CourseDetailActivity logic)
        val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        val tvOriginalPrice = view.findViewById<TextView>(R.id.tvOriginalPrice)
        val tvDiscount = view.findViewById<TextView>(R.id.tvDiscount)
        
        course?.pricing?.let { pricing ->
            val originalPrice = pricing.price
            val discountedPrice = pricing.discountedPrice
            
            // Use discountedPrice if available, otherwise use original price
            val finalPrice = if (discountedPrice > 0) discountedPrice else originalPrice
            
            // Show final price as main price
            tvPrice.text = "₹${finalPrice.toInt()}"
            
            // Show original price with strikethrough only if there's a discounted price
            if (discountedPrice > 0 && discountedPrice < originalPrice) {
                tvOriginalPrice.text = "₹${originalPrice.toInt()}"
                tvOriginalPrice.visibility = View.VISIBLE
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
        
        // Add default course features
        val llFeatures = view.findViewById<android.widget.LinearLayout>(R.id.llFeatures)
        llFeatures.removeAllViews()
        
        // Use default features since course.features doesn't exist
        val defaultFeatures = listOf(
            "Access all course content",
            "Take all practice tests",
            "Download study materials"
        )
        
        defaultFeatures.forEach { feature ->
            val featureLayout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dpToPx()
                }
            }
            
            val checkIcon = android.widget.ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_check_circle)
                setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
                layoutParams = android.widget.LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                    marginEnd = 12.dpToPx()
                }
            }
            
            val featureText = TextView(requireContext()).apply {
                text = feature
                textSize = 14f
                val typedValue = android.util.TypedValue()
                context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                setTextColor(typedValue.data)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            
            featureLayout.addView(checkIcon)
            featureLayout.addView(featureText)
            llFeatures.addView(featureLayout)
        }
        
        // Enroll Now button
        view.findViewById<MaterialButton>(R.id.btnEnrollNow).setOnClickListener {
            bottomSheetDialog.dismiss()
            // Navigate to CheckoutActivity with course data
            val intent = Intent(requireContext(), CheckoutActivity::class.java)
            intent.putExtra("COURSE_ID", course?.id)
            intent.putExtra("COURSE_NAME", course?.basicInfo?.name)
            intent.putExtra("COURSE_PRICE", course?.pricing?.price)
            startActivity(intent)
        }
        
        // Maybe Later button
        view.findViewById<MaterialButton>(R.id.btnMaybeLater).setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ARG_COURSE = "course"
        private const val ARG_IS_ENROLLED = "is_enrolled"

        fun newInstance(course: Course, isEnrolled: Boolean) =
            CourseContentFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COURSE, course)
                    putBoolean(ARG_IS_ENROLLED, isEnrolled)
                }
            }
    }
}
