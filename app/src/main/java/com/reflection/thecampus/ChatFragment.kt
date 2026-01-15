package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.reflection.thecampus.adapter.CourseChatListAdapter
import com.reflection.thecampus.data.model.CourseChatPreview
import com.reflection.thecampus.ui.chat.CourseChatActivity
import timber.log.Timber

class ChatFragment : Fragment() {

    private lateinit var rvChatList: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var ivEmptyIcon: ImageView
    private lateinit var shimmerChatList: com.facebook.shimmer.ShimmerFrameLayout

    private lateinit var adapter: CourseChatListAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private var enrolledCourses = listOf<Course>()
    private val chatPreviews = mutableListOf<CourseChatPreview>()
    private val messageListeners = mutableMapOf<String, ValueEventListener>()
    
    // Chat feature status
    private var isChatFeatureActive = true
    private var chatFeatureListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Handle window insets to prevent content being hidden behind system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }

        // Initialize views
        rvChatList = view.findViewById(R.id.rvChatList)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)
        ivEmptyIcon = view.findViewById(R.id.ivEmptyIcon)
        shimmerChatList = view.findViewById(R.id.shimmerChatList)

        setupRecyclerView()
        checkGlobalChatFeatureStatus()
        loadEnrolledCourses()

        return view
    }

    private fun setupRecyclerView() {
        adapter = CourseChatListAdapter { chatPreview ->
            openCourseChat(chatPreview)
        }
        
        rvChatList.layoutManager = LinearLayoutManager(context)
        rvChatList.adapter = adapter
    }

    private fun openCourseChat(chatPreview: CourseChatPreview) {
        val intent = Intent(requireContext(), CourseChatActivity::class.java)
        intent.putExtra("COURSE_ID", chatPreview.courseId)
        intent.putExtra("COURSE_NAME", chatPreview.courseName)
        startActivity(intent)
    }

    private fun loadEnrolledCourses() {
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            showNoCourses()
            return
        }

        // Show shimmer
        shimmerChatList.visibility = View.VISIBLE
        shimmerChatList.startShimmer()
        rvChatList.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        database.getReference("users/$userId/enrolledCourses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val courseIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.key?.let { courseIds.add(it) }
                    }

                    if (courseIds.isEmpty()) {
                        shimmerChatList.stopShimmer()
                        shimmerChatList.visibility = View.GONE
                        showNoCourses()
                        return
                    }

                    loadCourseDetails(courseIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error loading enrolled courses: ${error.message}")
                    shimmerChatList.stopShimmer()
                    shimmerChatList.visibility = View.GONE
                    showNoCourses()
                }
            })
    }

    private fun loadCourseDetails(courseIds: List<String>) {
        val courses = mutableListOf<Course>()
        var loadedCount = 0

        courseIds.forEach { courseId ->
            database.getReference("courses/$courseId")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue(Course::class.java)?.let { course ->
                            val courseWithId = course.copy(id = courseId)
                            courses.add(courseWithId)
                        }
                        
                        loadedCount++
                        if (loadedCount == courseIds.size) {
                            enrolledCourses = courses
                            
                            // Stop shimmer
                            shimmerChatList.stopShimmer()
                            shimmerChatList.visibility = View.GONE
                            
                            if (courses.isEmpty()) {
                                showNoCourses()
                            } else {
                                rvChatList.visibility = View.VISIBLE
                                loadChatPreviews(courses)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Timber.e("Error loading course: ${error.message}")
                        loadedCount++
                        if (loadedCount == courseIds.size) {
                            shimmerChatList.stopShimmer()
                            shimmerChatList.visibility = View.GONE
                            if (courses.isEmpty()) {
                                showNoCourses()
                            } else {
                                rvChatList.visibility = View.VISIBLE
                                loadChatPreviews(courses)
                            }
                        }
                    }
                })
        }
    }

    private fun loadChatPreviews(courses: List<Course>) {
        chatPreviews.clear()
        
        courses.forEach { course ->
            // Listen for ONLY the last message in each course chat (bandwidth optimization)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Get the single last message
                    val lastMessage = snapshot.children.firstOrNull()?.let { child ->
                        child.getValue(com.reflection.thecampus.data.model.GroupChatMessage::class.java)?.copy(id = child.key ?: "")
                    }
                    
                    val currentUserId = auth.currentUser?.uid ?: ""
                    
                    val preview = CourseChatPreview(
                        courseId = course.id,
                        courseName = course.basicInfo.name,
                        lastMessage = lastMessage?.text ?: "No messages yet",
                        lastMessageTime = lastMessage?.timestamp ?: 0,
                        lastMessageSender = if (lastMessage?.senderId == currentUserId) "You" else lastMessage?.senderName ?: "",
                        courseImageUrl = course.pricing.thumbnailUrl
                    )
                    
                    // Update or add preview
                    val existingIndex = chatPreviews.indexOfFirst { it.courseId == course.id }
                    if (existingIndex >= 0) {
                        chatPreviews[existingIndex] = preview
                    } else {
                        chatPreviews.add(preview)
                    }
                    
                    adapter.submitList(chatPreviews.toList())
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error loading chat preview for ${course.id}: ${error.message}")
                }
            }
            
            messageListeners[course.id] = listener
            // OPTIMIZATION: Fetch only the last message using limitToLast(1)
            database.getReference("courseChats/${course.id}/messages")
                .orderByKey()
                .limitToLast(1)
                .addValueEventListener(listener)
        }
    }

    private fun showNoCourses() {
        rvChatList.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        tvEmptyTitle.text = "No Courses Enrolled"
        tvEmptySubtitle.text = "Enroll in courses to start chatting"
        ivEmptyIcon.setImageResource(R.drawable.book_open_svgrepo_com)
    }
    
    private fun checkGlobalChatFeatureStatus() {
        chatFeatureListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isChatFeatureActive = snapshot.getValue(Boolean::class.java) ?: true
                
                if (!isChatFeatureActive) {
                    showMaintenanceState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error checking chat feature status: ${error.message}")
                isChatFeatureActive = true
            }
        }
        
        database.getReference("siteSettings/appControls/chatFeature/isActive")
            .addValueEventListener(chatFeatureListener!!)
    }
    
    private fun showMaintenanceState() {
        rvChatList.visibility = View.GONE
        shimmerChatList.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        
        tvEmptyTitle.text = "Chat Unavailable"
        tvEmptySubtitle.text = "Chat feature is under maintenance"
        ivEmptyIcon.setImageResource(R.drawable.ic_settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Remove all message listeners
        messageListeners.forEach { (courseId, listener) ->
            database.getReference("courseChats/$courseId/messages").removeEventListener(listener)
        }
        messageListeners.clear()
        
        // Remove chat feature listener
        chatFeatureListener?.let {
            database.getReference("siteSettings/appControls/chatFeature/isActive").removeEventListener(it)
        }
        
        shimmerChatList.stopShimmer()
    }
}
