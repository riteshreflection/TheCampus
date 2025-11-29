package com.reflection.thecampus.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.ChatMessage
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var chatAdapter: ChatAdapter
    
    private lateinit var database: DatabaseReference
    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var mentorId: String
    private lateinit var mentorName: String
    private lateinit var courseName: String
    
    private val messagesList = mutableListOf<ChatMessage>()
    private var messagesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get data from intent
        mentorId = intent.getStringExtra("mentorId") ?: ""
        mentorName = intent.getStringExtra("mentorName") ?: "Mentor"
        courseName = intent.getStringExtra("courseName") ?: ""
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (mentorId.isEmpty() || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error initializing chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Generate Chat ID: sort uids alphabetically and join with underscore
        val uids = listOf(currentUserId, mentorId).sorted()
        chatId = "${uids[0]}_${uids[1]}"
        
        database = FirebaseDatabase.getInstance().getReference("mentorChats").child(chatId)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupMessageInput()
    }
    
    override fun onStart() {
        super.onStart()
        listenForMessages()
    }
    
    override fun onStop() {
        super.onStop()
        messagesListener?.let { database.removeEventListener(it) }
        messagesListener = null
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        
        findViewById<TextView>(R.id.tvMentorName).text = mentorName
        findViewById<TextView>(R.id.tvCourseName).text = courseName
    }

    private fun initializeViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = chatAdapter
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
        
        // Initial state
        btnSend.isEnabled = false
        btnSend.alpha = 0.5f

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val rawText = etMessage.text.toString().trim()
        if (rawText.isEmpty()) return
        
        // Sanitize message to prevent XSS
        val text = com.reflection.thecampus.utils.InputValidator.sanitizeChatMessage(rawText)

        val messageId = database.push().key ?: return
        val timestamp = System.currentTimeMillis()
        
        val message = ChatMessage(
            text = text,
            timestamp = timestamp,
            senderId = currentUserId,
            isRead = false,
            courseName = courseName
        )

        database.child(messageId).setValue(message)
            .addOnSuccessListener {
                etMessage.setText("")
                rvMessages.scrollToPosition(messagesList.size - 1)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    if (message != null) {
                        messagesList.add(message)
                        
                        // Mark as read if sender is mentor and not read yet
                        if (message.senderId == mentorId && !message.isRead) {
                            child.ref.child("isRead").setValue(true)
                        }
                    }
                }
                chatAdapter.setMessages(messagesList)
                if (messagesList.isNotEmpty()) {
                    rvMessages.scrollToPosition(messagesList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.e(error.toException(), "Failed to load messages")
                Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        }
        
        database.addValueEventListener(messagesListener!!)
    }
}
