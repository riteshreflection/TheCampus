package com.reflection.thecampus

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.reflection.thecampus.adapter.GroupChatAdapter
import com.reflection.thecampus.data.model.GroupChatMessage
import com.reflection.thecampus.utils.SwipeToReplyCallback
import timber.log.Timber

class ChatFragment : Fragment() {

    private lateinit var spinnerCourses: Spinner
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var layoutEmpty: View
    private lateinit var tvTypingIndicator: TextView
    private lateinit var cardReplyPreview: CardView
    private lateinit var tvReplyPreviewText: TextView
    private lateinit var btnCancelReply: ImageView

    private lateinit var adapter: GroupChatAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private var currentCourseId: String? = null
    private var enrolledCourses = listOf<Course>()
    private var messagesListener: ValueEventListener? = null
    private var replyToMessage: GroupChatMessage? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Initialize views
        spinnerCourses = view.findViewById(R.id.spinnerCourses)
        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvTypingIndicator = view.findViewById(R.id.tvTypingIndicator)
        cardReplyPreview = view.findViewById(R.id.cardReplyPreview)
        tvReplyPreviewText = view.findViewById(R.id.tvReplyPreviewText)
        btnCancelReply = view.findViewById(R.id.btnCancelReply)

        setupRecyclerView()
        setupMessageInput()
        setupReplyPreview()
        loadEnrolledCourses()

        return view
    }

    private fun setupRecyclerView() {
        val userId = auth.currentUser?.uid ?: ""
        adapter = GroupChatAdapter(userId) { message, view ->
            showReactionDialog(message, view)
        }
        
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter
        
        // Setup swipe-to-reply
        val swipeCallback = SwipeToReplyCallback { position ->
            adapter.getMessageAt(position)?.let { message ->
                setReplyTo(message)
            }
            // Notify adapter to reset view position
            adapter.notifyItemChanged(position)
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(rvMessages)
    }

    private fun setupMessageInput() {
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSend.isEnabled = !s.isNullOrBlank()
                btnSend.alpha = if (s.isNullOrBlank()) 0.5f else 1.0f
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.isEnabled = false
        btnSend.alpha = 0.5f

        btnSend.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun setupReplyPreview() {
        btnCancelReply.setOnClickListener {
            clearReply()
        }
    }
    
    private fun setReplyTo(message: GroupChatMessage) {
        replyToMessage = message
        tvReplyPreviewText.text = message.text
        cardReplyPreview.visibility = View.VISIBLE
    }
    
    private fun clearReply() {
        replyToMessage = null
        cardReplyPreview.visibility = View.GONE
    }
    
    private fun showReactionDialog(message: GroupChatMessage, anchorView: View) {
        val userId = auth.currentUser?.uid ?: return
        val canEdit = message.senderId == userId
        val canDelete = message.senderId == userId
        
        val dialog = com.reflection.thecampus.ui.dialogs.ReactionBarDialog(
            requireContext(),
            onReactionSelected = { emoji ->
                addReaction(message, emoji)
            },
            onEdit = if (canEdit) {{ editMessage(message) }} else null,
            onDelete = if (canDelete) {{ deleteMessage(message) }} else null
        )
        
        // Position dialog near the message
        dialog.show()
        dialog.window?.let { window ->
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            
            val params = window.attributes
            params.y = location[1] - anchorView.height / 2
            window.attributes = params
        }
    }
    
    private fun addReaction(message: GroupChatMessage, emoji: String) {
        val courseId = currentCourseId ?: return
        val userId = auth.currentUser?.uid ?: return
        
        database.getReference("courseChats/$courseId/messages/${message.id}/reactions/$userId")
            .setValue(emoji)
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add reaction", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun editMessage(message: GroupChatMessage) {
        val editText = EditText(requireContext())
        editText.setText(message.text)
        editText.setSelection(message.text.length)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val courseId = currentCourseId ?: return@setPositiveButton
                    database.getReference("courseChats/$courseId/messages/${message.id}/text")
                        .setValue(newText)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Message updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update message", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteMessage(message: GroupChatMessage) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                val courseId = currentCourseId ?: return@setPositiveButton
                database.getReference("courseChats/$courseId/messages/${message.id}")
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadEnrolledCourses() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users/$userId/enrolledCourses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val courseIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.key?.let { courseIds.add(it) }
                    }

                    if (courseIds.isEmpty()) {
                        showNoCourses()
                        return
                    }

                    loadCourseDetails(courseIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error loading enrolled courses: ${error.message}")
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
                            courses.add(course)
                        }
                        
                        loadedCount++
                        if (loadedCount == courseIds.size) {
                            enrolledCourses = courses
                            setupCourseSpinner(courses)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Timber.e("Error loading course: ${error.message}")
                    }
                })
        }
    }

    private fun setupCourseSpinner(courses: List<Course>) {
        val courseNames = courses.map { it.basicInfo.name }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            courseNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourses.adapter = spinnerAdapter

        spinnerCourses.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCourseId = courses[position].id
                
                // Only reload if course actually changed
                if (currentCourseId != newCourseId) {
                    // 1. Remove existing listener immediately
                    messagesListener?.let { listener ->
                        currentCourseId?.let { oldId ->
                            database.getReference("courseChats/$oldId/messages").removeEventListener(listener)
                        }
                    }
                    messagesListener = null
                    
                    // 2. Clear UI immediately
                    adapter.setMessages(emptyList())
                    clearReply()
                    layoutEmpty.visibility = View.VISIBLE
                    rvMessages.visibility = View.GONE
                    
                    // 3. Set new course ID and load
                    currentCourseId = newCourseId
                    loadMessagesForCourse(newCourseId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadMessagesForCourse(courseId: String) {
        // Listener is already removed in onItemSelected, but double check to be safe
        messagesListener?.let { listener ->
             database.getReference("courseChats").removeEventListener(listener) // Failsafe
        }

        // Listen for messages in NEW course
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<GroupChatMessage>()
                
                snapshot.children.forEach { child ->
                    child.getValue(GroupChatMessage::class.java)?.let { message ->
                        messages.add(message.copy(id = child.key ?: ""))
                    }
                }

                adapter.setMessages(messages.sortedBy { it.timestamp })
                
                if (messages.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvMessages.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvMessages.visibility = View.VISIBLE
                    rvMessages.scrollToPosition(messages.size - 1)
                }
                
                Timber.d("Loaded ${messages.size} messages for course: $courseId")
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error loading messages: ${error.message}")
            }
        }

        database.getReference("courseChats/$courseId/messages")
            .addValueEventListener(messagesListener!!)
    }

    private fun sendMessage() {
        val courseId = currentCourseId ?: return
        val userId = auth.currentUser?.uid ?: return
        val text = etMessage.text.toString().trim()
        
        if (text.isEmpty()) return

        val messageRef = database.getReference("courseChats/$courseId/messages").push()
        val messageId = messageRef.key ?: return

        // Get user name
        database.getReference("userProfiles/$userId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("fullName").getValue(String::class.java) ?: "Unknown"

                    val message = GroupChatMessage(
                        id = messageId,
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        senderId = userId,
                        senderName = userName,
                        courseId = courseId,
                        replyToId = replyToMessage?.id,
                        replyToText = replyToMessage?.text,
                        replyToSender = replyToMessage?.senderName
                    )

                    messageRef.setValue(message)
                        .addOnSuccessListener {
                            etMessage.setText("")
                            clearReply()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error getting user name: ${error.message}")
                }
            })
    }

    private fun showNoCourses() {
        Toast.makeText(context, "You are not enrolled in any courses", Toast.LENGTH_LONG).show()
        layoutEmpty.visibility = View.VISIBLE
        rvMessages.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.let {
            currentCourseId?.let { courseId ->
                database.getReference("courseChats/$courseId/messages").removeEventListener(it)
            }
        }
    }
}
