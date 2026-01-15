package com.reflection.thecampus.ui.chat

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.reflection.thecampus.R
import com.reflection.thecampus.adapter.GroupChatAdapter
import com.reflection.thecampus.data.model.GroupChatMessage
import com.reflection.thecampus.utils.SwipeToReplyCallback
import timber.log.Timber

class CourseChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var layoutEmpty: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var ivEmptyIcon: ImageView
    private lateinit var tvTypingIndicator: TextView
    private lateinit var cardReplyPreview: CardView
    private lateinit var tvReplyPreviewText: TextView
    private lateinit var btnCancelReply: ImageView
    private lateinit var fabScrollToBottom: com.google.android.material.floatingactionbutton.FloatingActionButton

    private lateinit var adapter: GroupChatAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private lateinit var courseId: String
    private lateinit var courseName: String
    private var messagesListener: ValueEventListener? = null
    private var replyToMessage: GroupChatMessage? = null
    private var countdownTimer: android.os.CountDownTimer? = null
    
    // Pagination variables
    private var isLoadingMore = false
    private var oldestMessageKey: String? = null
    private val PAGE_SIZE = 20
    private val allMessages = mutableListOf<GroupChatMessage>()
    
    // Chat feature status
    private var isChatFeatureActive = true
    private var isCourseChatActive = true
    private var chatFeatureListener: ValueEventListener? = null
    private var courseChatListener: ValueEventListener? = null
    
    // Mute status
    private var isMuted = false
    private var muteListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_course_chat)
        
        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkMode
        controller.isAppearanceLightNavigationBars = !isDarkMode
        
        // Apply window insets to handle keyboard properly
        val rootLayout = findViewById<View>(R.id.toolbar).parent as View
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            
            // Apply the larger of the two (nav bar or keyboard) to bottom padding
            val bottomPadding = kotlin.math.max(systemBars.bottom, ime.bottom)
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, bottomPadding)
            insets
        }

        // Get intent extras
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        courseName = intent.getStringExtra("COURSE_NAME") ?: "Course Chat"
        
        if (courseId.isEmpty()) {
            Toast.makeText(this, "Invalid course", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        initViews()
        setupRecyclerView()
        setupMessageInput()
        setupReplyPreview()
        setupScrollToBottomFab()
        checkGlobalChatFeatureStatus()
        loadMessages()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = courseName
        toolbar.setNavigationOnClickListener { finish() }
        
        // Listen for mute status changes
        listenForMuteStatus()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        ivEmptyIcon = findViewById(R.id.ivEmptyIcon)
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator)
        cardReplyPreview = findViewById(R.id.cardReplyPreview)
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText)
        btnCancelReply = findViewById(R.id.btnCancelReply)
        fabScrollToBottom = findViewById(R.id.fabScrollToBottom)
    }

    private fun setupRecyclerView() {
        val userId = auth.currentUser?.uid ?: ""
        adapter = GroupChatAdapter(userId) { message, view ->
            showReactionDialog(message, view)
        }
        
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter
        
        // Setup swipe-to-reply
        val swipeCallback = SwipeToReplyCallback { position ->
            adapter.getMessageAt(position)?.let { message ->
                setReplyTo(message)
            }
            adapter.notifyItemChanged(position)
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(rvMessages)
        
        // Setup scroll listener for pagination
        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutMgr = recyclerView.layoutManager as? LinearLayoutManager
                val firstVisiblePosition = layoutMgr?.findFirstVisibleItemPosition() ?: 0
                
                // Load more when near top (first 5 items visible)
                if (firstVisiblePosition < 5 && !isLoadingMore && oldestMessageKey != null) {
                    loadOlderMessages()
                }
            }
        })
    }

    private fun setupMessageInput() {
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                btnSend.isEnabled = hasText
                btnSend.alpha = if (hasText) 1.0f else 0.5f
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

    private fun setupScrollToBottomFab() {
        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val lastVisiblePosition = layoutManager?.findLastCompletelyVisibleItemPosition() ?: 0
                val totalItems = adapter.itemCount
                
                if (totalItems > 0 && lastVisiblePosition < totalItems - 1) {
                    fabScrollToBottom.show()
                } else {
                    fabScrollToBottom.hide()
                }
            }
        })
        
        fabScrollToBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }
    }
    
    private fun setReplyTo(message: GroupChatMessage) {
        replyToMessage = message
        tvReplyPreviewText.text = message.text
        cardReplyPreview.visibility = View.VISIBLE
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_course_chat, menu)
        updateMuteIcon(menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mute -> {
                toggleMute()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun listenForMuteStatus() {
        val userId = auth.currentUser?.uid ?: return
        
        muteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMuted = snapshot.getValue(Boolean::class.java) ?: false
                invalidateOptionsMenu() // Refresh menu to update icon
            }
            
            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error listening for mute status: ${error.message}")
            }
        }
        
        database.getReference("users/$userId/mutedChats/$courseId")
            .addValueEventListener(muteListener!!)
    }
    
    private fun updateMuteIcon(menu: Menu) {
        val muteItem = menu.findItem(R.id.action_mute)
        if (isMuted) {
            muteItem.setIcon(R.drawable.ic_notifications_off)
            muteItem.title = "Unmute notifications"
        } else {
            muteItem.setIcon(R.drawable.ic_notifications)
            muteItem.title = "Mute notifications"
        }
    }
    
    private fun toggleMute() {
        val userId = auth.currentUser?.uid ?: return
        val mutedRef = database.getReference("users/$userId/mutedChats/$courseId")
        
        mutedRef.setValue(!isMuted)
            .addOnSuccessListener {
                val message = if (!isMuted) {
                    "Notifications muted for $courseName"
                } else {
                    "Notifications enabled for $courseName"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update notification settings", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun clearReply() {
        replyToMessage = null
        cardReplyPreview.visibility = View.GONE
    }
    
    private fun showReactionDialog(message: GroupChatMessage, anchorView: View) {
        val userId = auth.currentUser?.uid ?: return
        val isOwnMessage = message.senderId == userId
        
        val bottomSheet = com.reflection.thecampus.ui.dialogs.MessageActionsBottomSheet(
            message = message,
            isOwnMessage = isOwnMessage,
            onReactionToggle = { emoji ->
                toggleReaction(message, emoji)
            },
            onReply = {
                setReplyTo(message)
            },
            onEdit = if (isOwnMessage) {{ editMessage(message) }} else null,
            onDelete = if (isOwnMessage) {{ deleteMessage(message) }} else null,
            onReport = if (!isOwnMessage) {{ reportMessage(message) }} else null
        )
        
        bottomSheet.show(supportFragmentManager, com.reflection.thecampus.ui.dialogs.MessageActionsBottomSheet.TAG)
    }
    
    private fun toggleReaction(message: GroupChatMessage, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val reactionsRef = database.getReference("courseChats/$courseId/messages/${message.id}/reactions")
        
        val existingReaction = message.reactions.entries.find { 
            it.key == userId && it.value == emoji 
        }
        
        if (existingReaction != null) {
            reactionsRef.child(userId).removeValue()
        } else {
            reactionsRef.child(userId).setValue(emoji)
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add reaction", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun editMessage(message: GroupChatMessage) {
        val editText = EditText(this)
        editText.setText(message.text)
        editText.setSelection(message.text.length)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    database.getReference("courseChats/$courseId/messages/${message.id}/text")
                        .setValue(newText)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Message updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update message", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteMessage(message: GroupChatMessage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                database.getReference("courseChats/$courseId/messages/${message.id}")
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportMessage(message: GroupChatMessage) {
        val userId = auth.currentUser?.uid ?: return
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Report Message")
            .setMessage("Report this message as inappropriate?")
            .setPositiveButton("Report") { _, _ ->
                val reportRef = database.getReference("messageReports").push()
                val report = mapOf(
                    "messageId" to message.id,
                    "courseId" to courseId,
                    "reportedBy" to userId,
                    "reportedAt" to System.currentTimeMillis(),
                    "messageText" to message.text,
                    "messageSenderId" to message.senderId
                )
                
                reportRef.setValue(report)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Message reported", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to report message", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadMessages() {
        checkCourseChatStatus(courseId)
        
        if (!isChatFeatureActive || !isCourseChatActive) {
            showMaintenanceState()
            return
        }
        
        // OPTIMIZATION: Load only the newest PAGE_SIZE messages initially
        database.getReference("courseChats/$courseId/messages")
            .orderByKey()
            .limitToLast(PAGE_SIZE)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isChatFeatureActive || !isCourseChatActive) {
                        showMaintenanceState()
                        return
                    }
                    
                    allMessages.clear()
                    
                    snapshot.children.forEach { child ->
                        child.getValue(GroupChatMessage::class.java)?.let { message ->
                            allMessages.add(message.copy(id = child.key ?: ""))
                        }
                    }
                    
                    // Store the oldest message key for pagination
                    oldestMessageKey = allMessages.firstOrNull()?.id
                    
                    adapter.setMessages(allMessages.sortedBy { it.timestamp })
                    
                    if (allMessages.isEmpty()) {
                        showEmptyState()
                    } else {
                        layoutEmpty.visibility = View.GONE
                        rvMessages.visibility = View.VISIBLE
                        rvMessages.scrollToPosition(allMessages.size - 1)
                    }
                    
                    // Listen for new messages in real-time
                    listenForNewMessages()
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error loading initial messages: ${error.message}")
                }
            })
    }
    
    private fun listenForNewMessages() {
        // Listen for new messages added after the initial load
        val newestMessageKey = allMessages.lastOrNull()?.id ?: return
        
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasNewMessages = false
                
                snapshot.children.forEach { child ->
                    val messageId = child.key ?: return@forEach
                    
                    // Only add if it's a new message (not already in our list)
                    if (allMessages.none { it.id == messageId }) {
                        child.getValue(GroupChatMessage::class.java)?.let { message ->
                            allMessages.add(message.copy(id = messageId))
                            hasNewMessages = true
                        }
                    }
                }
                
                if (hasNewMessages) {
                    adapter.setMessages(allMessages.sortedBy { it.timestamp })
                    rvMessages.scrollToPosition(allMessages.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error listening for new messages: ${error.message}")
            }
        }
        
        database.getReference("courseChats/$courseId/messages")
            .orderByKey()
            .startAfter(newestMessageKey)
            .addValueEventListener(messagesListener!!)
    }
    
    private fun loadOlderMessages() {
        if (isLoadingMore || oldestMessageKey == null) return
        
        isLoadingMore = true
        
        database.getReference("courseChats/$courseId/messages")
            .orderByKey()
            .endBefore(oldestMessageKey)
            .limitToLast(PAGE_SIZE)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val olderMessages = mutableListOf<GroupChatMessage>()
                    
                    snapshot.children.forEach { child ->
                        child.getValue(GroupChatMessage::class.java)?.let { message ->
                            olderMessages.add(message.copy(id = child.key ?: ""))
                        }
                    }
                    
                    if (olderMessages.isNotEmpty()) {
                        // Update oldest message key
                        oldestMessageKey = olderMessages.firstOrNull()?.id
                        
                        // Add older messages to the beginning
                        allMessages.addAll(0, olderMessages)
                        adapter.setMessages(allMessages.sortedBy { it.timestamp })
                        
                        // Maintain scroll position
                        val layoutManager = rvMessages.layoutManager as? LinearLayoutManager
                        layoutManager?.scrollToPositionWithOffset(olderMessages.size, 0)
                    } else {
                        // No more older messages
                        oldestMessageKey = null
                    }
                    
                    isLoadingMore = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error loading older messages: ${error.message}")
                    isLoadingMore = false
                }
            })
    }

    private fun sendMessage() {
        val userId = auth.currentUser?.uid ?: return
        val text = etMessage.text.toString().trim()
        
        if (text.isEmpty()) return

        // Validate message
        val validationResult = com.reflection.thecampus.utils.MessageValidator.validateMessage(text)
        if (!validationResult.isValid) {
            if (validationResult.remainingTimeSeconds > 0) {
                showRateLimitTimer(validationResult.errorMessage ?: "Rate limit", validationResult.remainingTimeSeconds)
            } else {
                Toast.makeText(this, validationResult.errorMessage, Toast.LENGTH_SHORT).show()
            }
            return
        }

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
                            Toast.makeText(this@CourseChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Error getting user name: ${error.message}")
                }
            })
    }

    private fun showRateLimitTimer(message: String, seconds: Int) {
        countdownTimer?.cancel()
        
        btnSend.isEnabled = false
        btnSend.alpha = 0.5f
        
        countdownTimer = object : android.os.CountDownTimer((seconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val timeText = if (secondsLeft >= 60) {
                    "${secondsLeft / 60}m ${secondsLeft % 60}s"
                } else {
                    "${secondsLeft}s"
                }
                etMessage.hint = "$message $timeText"
            }

            override fun onFinish() {
                etMessage.hint = "Type a message..."
                btnSend.isEnabled = etMessage.text.isNotBlank()
                btnSend.alpha = if (etMessage.text.isNotBlank()) 1.0f else 0.5f
            }
        }.start()
    }
    
    private fun checkGlobalChatFeatureStatus() {
        chatFeatureListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isChatFeatureActive = snapshot.getValue(Boolean::class.java) ?: true
                
                if (!isChatFeatureActive) {
                    showMaintenanceState()
                } else if (courseId.isNotEmpty()) {
                    checkCourseChatStatus(courseId)
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
    
    private fun checkCourseChatStatus(courseId: String) {
        courseChatListener?.let {
            database.getReference("siteSettings/appControls/chatFeature").removeEventListener(it)
        }
        
        courseChatListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isCourseChatActive = snapshot.getValue(Boolean::class.java) ?: true
                
                if (!isChatFeatureActive || !isCourseChatActive) {
                    showMaintenanceState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error checking course chat status: ${error.message}")
                isCourseChatActive = true
            }
        }
        
        database.getReference("siteSettings/appControls/chatFeature/$courseId")
            .addValueEventListener(courseChatListener!!)
    }
    
    private fun showMaintenanceState() {
        layoutEmpty.visibility = View.VISIBLE
        rvMessages.visibility = View.GONE
        adapter.setMessages(emptyList())
        
        tvEmptyTitle.text = "Chat Unavailable"
        tvEmptySubtitle.text = "Chat feature is under maintenance"
        ivEmptyIcon.setImageResource(R.drawable.ic_settings)
        
        etMessage.isEnabled = false
        btnSend.isEnabled = false
        btnSend.alpha = 0.5f
        etMessage.hint = "Chat is currently disabled"
    }
    
    private fun showEmptyState() {
        layoutEmpty.visibility = View.VISIBLE
        rvMessages.visibility = View.GONE
        
        tvEmptyTitle.text = "No messages yet"
        tvEmptySubtitle.text = "Start a conversation!"
        ivEmptyIcon.setImageResource(R.drawable.ic_chat_bubble)
        
        etMessage.isEnabled = true
        etMessage.hint = "Type a message..."
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.let {
            database.getReference("courseChats/$courseId/messages").removeEventListener(it)
        }
        chatFeatureListener?.let {
            database.getReference("siteSettings/appControls/chatFeature/isActive").removeEventListener(it)
        }
        courseChatListener?.let {
            database.getReference("siteSettings/appControls/chatFeature").removeEventListener(it)
        }
        countdownTimer?.cancel()
    }
}
