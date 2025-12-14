Android Live Classes Implementation Guide
Complete guide for implementing student-side live class functionality in your Android app using Kotlin.

Overview
This guide enables Android developers to build the student-facing live class experience that connects to your admin portal's live streaming infrastructure. Students can discover, join, and participate in live classes with video, audio, and chat.

Prerequisites
Minimum SDK: Android 21 (Lollipop)
Target SDK: Android 34 (Latest)
Language: Kotlin
Architecture: MVVM recommended
Step 1: Add Dependencies
1.1 Add Agora SDK
In build.gradle.kts (Module: app):

dependencies {
    // Agora RTC SDK
    implementation("io.agora.rtc:full-sdk:4.3.1")
    
    // Firebase Realtime Database
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Compose (if using Jetpack Compose)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
}
1.2 Add Permissions
In AndroidManifest.xml:

<manifest>
    <!-- Agora Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    
    <!-- Optional: For better network detection -->
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
    
    <application>
        <!-- Activities -->
    </application>
</manifest>
Step 2: Firebase Data Models
2.1 Live Class Model
data class LiveClass(
    val id: String = "",
    val metadata: ClassMetadata = ClassMetadata(),
    val status: ClassStatus = ClassStatus.SCHEDULED,
    val recording: Recording? = null
)
data class ClassMetadata(
    val name: String = "",
    val description: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val instructorId: String = "",
    val instructorName: String = "",
    val scheduledAt: Long = 0L,
    val duration: Int = 60, // minutes
    val maxParticipants: Int = 50,
    val agoraChannelName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
enum class ClassStatus {
    SCHEDULED, LIVE, ENDED, CANCELLED
}
data class Recording(
    val enabled: Boolean = false,
    val status: RecordingStatus? = null,
    val fileUrl: String? = null,
    val duration: Int? = null
)
enum class RecordingStatus {
    RECORDING, PROCESSING, READY, FAILED
}
2.2 Participant Model
data class LiveClassParticipant(
    val uid: String = "",
    val name: String = "",
    val email: String? = null,
    val role: ParticipantRole = ParticipantRole.STUDENT,
    val joinedAt: Long = 0L,
    val leftAt: Long? = null,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isHandRaised: Boolean = false,
    val connectionQuality: ConnectionQuality? = null
)
enum class ParticipantRole {
    INSTRUCTOR, STUDENT
}
enum class ConnectionQuality {
    EXCELLENT, GOOD, POOR
}
2.3 Chat Message Model
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val message: String = "",
    val timestamp: Long = 0L,
    val deleted: Boolean = false
)
Step 3: API Integration
3.1 API Service Interface
interface AgoraApiService {
    @POST("api/agora/rtc-token")
    suspend fun getRtcToken(@Body request: TokenRequest): TokenResponse
}
data class TokenRequest(
    val channelName: String,
    val uid: String,
    val role: String = "publisher" // Always publisher for students
)
data class TokenResponse(
    val token: String,
    val uid: String,
    val channelName: String,
    val expiresAt: Long,
    val appId: String
)
3.2 Retrofit Setup
object ApiClient {
    private const val BASE_URL = "https://admin.thecampus.in/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val agoraApi: AgoraApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgoraApiService::class.java)
    }
}
Step 4: Firebase Repository
4.1 Live Classes Repository
class LiveClassRepository {
    private val database = Firebase.database
    private val auth = Firebase.auth
    
    // Get all live classes
    fun getLiveClasses(): Flow<List<LiveClass>> = callbackFlow {
        val ref = database.getReference("liveClasses")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val classes = snapshot.children.mapNotNull { child ->
                    child.getValue<LiveClass>()?.copy(id = child.key ?: "")
                }
                trySend(classes)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    // Get single live class
    suspend fun getLiveClass(classId: String): LiveClass? {
        return try {
            val snapshot = database.getReference("liveClasses/$classId")
                .get()
                .await()
            snapshot.getValue<LiveClass>()?.copy(id = classId)
        } catch (e: Exception) {
            null
        }
    }
    
    // Add participant
    suspend fun addParticipant(classId: String, participant: LiveClassParticipant) {
        val ref = database.getReference("liveClasses/$classId/participants/${participant.uid}")
        ref.setValue(participant).await()
    }
    
    // Update participant
    suspend fun updateParticipant(classId: String, uid: String, updates: Map<String, Any>) {
        val ref = database.getReference("liveClasses/$classId/participants/$uid")
        ref.updateChildren(updates).await()
    }
    
    // Remove participant (mark as left)
    suspend fun removeParticipant(classId: String, uid: String) {
        val ref = database.getReference("liveClasses/$classId/participants/$uid")
        ref.updateChildren(mapOf("leftAt" to System.currentTimeMillis())).await()
    }
    
    // Listen to chat
    fun getChatMessages(classId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = database.getReference("liveClasses/$classId/chat")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children
                    .mapNotNull { it.getValue<ChatMessage>()?.copy(id = it.key ?: "") }
                    .filter { !it.deleted }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    // Send chat message
    suspend fun sendChatMessage(classId: String, message: String) {
        val user = auth.currentUser ?: return
        val ref = database.getReference("liveClasses/$classId/chat").push()
        
        val chatMessage = ChatMessage(
            id = ref.key ?: "",
            userId = user.uid,
            userName = user.displayName ?: "Student",
            userAvatar = user.photoUrl?.toString(),
            message = sanitizeMessage(message),
            timestamp = System.currentTimeMillis(),
            deleted = false
        )
        
        ref.setValue(chatMessage).await()
    }
    
    private fun sanitizeMessage(message: String): String {
        return message
            .take(500)
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }
}
Step 5: Agora Manager
5.1 AgoraManager Class
class AgoraManager(private val context: Context) {
    companion object {
        private const val APP_ID = "57358326d5f64f7891e7cab411919da4"
    }
    
    private var rtcEngine: RtcEngine? = null
    private var localVideoTrack: VideoCanvas? = null
    
    var onRemoteUserJoined: ((Int) -> Unit)? = null
    var onRemoteUserLeft: ((Int) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    // Initialize Agora Engine
    fun initialize() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = APP_ID
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        onRemoteUserJoined?.invoke(uid)
                    }
                    
                    override fun onUserOffline(uid: Int, reason: Int) {
                        onRemoteUserLeft?.invoke(uid)
                    }
                    
                    override fun onError(err: Int) {
                        onError?.invoke("Agora error: $err")
                    }
                }
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableVideo()
        } catch (e: Exception) {
            onError?.invoke("Failed to initialize: ${e.message}")
        }
    }
    
    // Join channel
    suspend fun joinChannel(channelName: String, token: String, userId: String): Boolean {
        return try {
            rtcEngine?.apply {
                // Set channel profile to communication (2-way interaction)
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                
                // Enable dual stream for better quality
                enableDualStreamMode(true)
                
                // Join channel
                val options = ChannelMediaOptions().apply {
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                    publishCameraTrack = true
                    publishMicrophoneTrack = true
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                }
                
                joinChannel(token, channelName, userId.hashCode(), options)
            }
            true
        } catch (e: Exception) {
            onError?.invoke("Failed to join: ${e.message}")
            false
        }
    }
    
    // Setup local video
    fun setupLocalVideo(view: SurfaceView, userId: String) {
        localVideoTrack = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, userId.hashCode())
        rtcEngine?.setupLocalVideo(localVideoTrack)
        rtcEngine?.startPreview()
    }
    
    // Setup remote video
    fun setupRemoteVideo(view: SurfaceView, remoteUid: Int) {
        val videoCanvas = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid)
        rtcEngine?.setupRemoteVideo(videoCanvas)
    }
    
    // Toggle microphone
    fun toggleMicrophone(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }
    
    // Toggle camera
    fun toggleCamera(enabled: Boolean) {
        rtcEngine?.muteLocalVideoStream(!enabled)
    }
    
    // Switch camera
    fun switchCamera() {
        rtcEngine?.switchCamera()
    }
    
    // Leave channel
    fun leaveChannel() {
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
    }
    
    // Release resources
    fun destroy() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
Step 6: ViewModel
6.1 LiveClassViewModel
class LiveClassViewModel(
    private val repository: LiveClassRepository,
    private val apiService: AgoraApiService,
    private val agoraManager: AgoraManager
) : ViewModel() {
    
    private val _liveClass = MutableStateFlow<LiveClass?>(null)
    val liveClass: StateFlow<LiveClass?> = _liveClass.asStateFlow()
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _isCameraOn = MutableStateFlow(true)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()
    
    private val _isHandRaised = MutableStateFlow(false)
    val isHandRaised: StateFlow<Boolean> = _isHandRaised.asStateFlow()
    
    private val _remoteUsers = MutableStateFlow<Set<Int>>(emptySet())
    val remoteUsers: StateFlow<Set<Int>> = _remoteUsers.asStateFlow()
    
    init {
        agoraManager.onRemoteUserJoined = { uid ->
            _remoteUsers.update { it + uid }
        }
        
        agoraManager.onRemoteUserLeft = { uid ->
            _remoteUsers.update { it - uid }
        }
    }
    
    // Load live class
    fun loadLiveClass(classId: String) {
        viewModelScope.launch {
            val liveClass = repository.getLiveClass(classId)
            _liveClass.value = liveClass
            
            // Start listening to chat
            liveClass?.let {
                repository.getChatMessages(classId).collect { messages ->
                    _chatMessages.value = messages
                }
            }
        }
    }
    
    // Join class
    suspend fun joinClass(userId: String, userName: String, userEmail: String): Result<Unit> {
        return try {
            val liveClass = _liveClass.value ?: return Result.failure(Exception("Class not found"))
            
            // Get Agora token
            val tokenResponse = apiService.getRtcToken(
                TokenRequest(
                    channelName = liveClass.id,
                    uid = userId,
                    role = "publisher"
                )
            )
            
            // Join Agora channel
            val joined = agoraManager.joinChannel(
                channelName = liveClass.id,
                token = tokenResponse.token,
                userId = userId
            )
            
            if (!joined) {
                return Result.failure(Exception("Failed to join Agora channel"))
            }
            
            // Add to Firebase participants
            repository.addParticipant(
                classId = liveClass.id,
                participant = LiveClassParticipant(
                    uid = userId,
                    name = userName,
                    email = userEmail,
                    role = ParticipantRole.STUDENT,
                    joinedAt = System.currentTimeMillis(),
                    isMuted = false,
                    isCameraOn = true,
                    isHandRaised = false
                )
            )
            
            _isConnected.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Toggle microphone
    fun toggleMicrophone() {
        val newState = !_isMuted.value
        agoraManager.toggleMicrophone(newState)
        _isMuted.value = newState
        
        // Update Firebase
        updateParticipantState(mapOf("isMuted" to newState))
    }
    
    // Toggle camera
    fun toggleCamera() {
        val newState = !_isCameraOn.value
        agoraManager.toggleCamera(newState)
        _isCameraOn.value = newState
        
        // Update Firebase
        updateParticipantState(mapOf("isCameraOn" to newState))
    }
    
    // Toggle hand raise
    fun toggleHandRaise() {
        val newState = !_isHandRaised.value
        _isHandRaised.value = newState
        
        // Update Firebase
        updateParticipantState(mapOf("isHandRaised" to newState))
    }
    
    // Send chat message
    fun sendMessage(message: String) {
        viewModelScope.launch {
            _liveClass.value?.let { liveClass ->
                repository.sendChatMessage(liveClass.id, message)
            }
        }
    }
    
    // Leave class
    fun leaveClass(userId: String) {
        viewModelScope.launch {
            _liveClass.value?.let { liveClass ->
                repository.removeParticipant(liveClass.id, userId)
                agoraManager.leaveChannel()
                _isConnected.value = false
            }
        }
    }
    
    private fun updateParticipantState(updates: Map<String, Any>) {
        viewModelScope.launch {
            _liveClass.value?.let { liveClass ->
                val userId = Firebase.auth.currentUser?.uid ?: return@launch
                repository.updateParticipant(liveClass.id, userId, updates)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        agoraManager.destroy()
    }
}
Step 7: Activity/Fragment UI
7.1 LiveClassActivity Layout
<!-- res/layout/activity_live_class.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#1a1a1a">
    
    <!-- Instructor Video (Main) -->
    <FrameLayout
        android:id="@+id/instructorVideoContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/controlsLayout"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@id/chatContainer">
        
        <SurfaceView
            android:id="@+id/instructorVideoView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>
        
        <!-- Self Preview (PIP) -->
        <SurfaceView
            android:id="@+id/localVideoView"
            android:layout_width="120dp"
            android:layout_height="160dp"
            android:layout_gravity="bottom|end"
            android:layout_margin="16dp"/>
    </FrameLayout>
    
    <!-- Chat Container -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/chatContainer"
        android:layout_width="300dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/chatInput"
        app:layout_constraintEnd_toEndOf="parent"
        android:background="#252525"
        android:padding="8dp"/>
    
    <!-- Chat Input -->
    <LinearLayout
        android:id="@+id/chatInput"
        android:layout_width="300dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#252525"
        app:layout_constraintBottom_toTopOf="@id/controlsLayout"
        app:layout_constraintEnd_toEndOf="parent">
        
        <EditText
            android:id="@+id/messageInput"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="Type a message..."
            android:textColor="#ffffff"
            android:textColorHint="#888888"/>
        
        <ImageButton
            android:id="@+id/sendButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:src="@drawable/ic_send"
            android:background="?attr/selectableItemBackgroundBorderless"/>
    </LinearLayout>
    
    <!-- Controls -->
    <LinearLayout
        android:id="@+id/controlsLayout"
        android:layout_width="0dp"
        android:layout_height="80dp"
        android:orientation="horizontal"
        android:gravity="center"
        android:background="#1f1f1f"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">
        
        <ImageButton
            android:id="@+id/micButton"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_mic"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_margin="8dp"/>
        
        <ImageButton
            android:id="@+id/cameraButton"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_camera"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_margin="8dp"/>
        
        <ImageButton
            android:id="@+id/handRaiseButton"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_hand"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_margin="8dp"/>
        
        <ImageButton
            android:id="@+id/leaveButton"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_call_end"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_margin="8dp"
            android:tint="#ff3b30"/>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
7.2 LiveClassActivity
class LiveClassActivity : AppCompatActivity() {
    
    private lateinit var viewModel: LiveClassViewModel
    private lateinit var agoraManager: AgoraManager
    private lateinit var binding: ActivityLiveClassBinding
    
    private val classId: String by lazy {
        intent.getStringExtra("CLASS_ID") ?: ""
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveClassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Request permissions
        requestPermissions()
        
        // Initialize Agora
        agoraManager = AgoraManager(this)
        agoraManager.initialize()
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            LiveClassViewModelFactory(agoraManager)
        )[LiveClassViewModel::class.java]
        
        // Setup UI
        setupObservers()
        setupControls()
        
        // Load class
        viewModel.loadLiveClass(classId)
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.liveClass.collect { liveClass ->
                liveClass?.let {
                    // Join class
                    joinClass(it)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.chatMessages.collect { messages ->
                // Update chat adapter
            }
        }
        
        lifecycleScope.launch {
            viewModel.remoteUsers.collect { users ->
                // Setup remote video for instructor
                users.firstOrNull()?.let { uid ->
                    agoraManager.setupRemoteVideo(binding.instructorVideoView, uid)
                }
            }
        }
    }
    
    private fun setupControls() {
        binding.micButton.setOnClickListener {
            viewModel.toggleMicrophone()
        }
        
        binding.cameraButton.setOnClickListener {
            viewModel.toggleCamera()
        }
        
        binding.handRaiseButton.setOnClickListener {
            viewModel.toggleHandRaise()
        }
        
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                binding.messageInput.text.clear()
            }
        }
        
        binding.leaveButton.setOnClickListener {
            finish()
        }
    }
    
    private fun joinClass(liveClass: LiveClass) {
        lifecycleScope.launch {
            val user = Firebase.auth.currentUser ?: return@launch
            
            // Setup local video
            agoraManager.setupLocalVideo(binding.localVideoView, user.uid)
            
            // Join channel
            val result = viewModel.joinClass(
                userId = user.uid,
                userName = user.displayName ?: "Student",
                userEmail = user.email ?: ""
            )
            
            result.onFailure { error ->
                Toast.makeText(this@LiveClassActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            100
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        val user = Firebase.auth.currentUser
        user?.let {
            viewModel.leaveClass(it.uid)
        }
    }
}
Step 8: Testing
8.1 Test Checklist
 Permissions granted for camera and microphone
 Firebase connection working
 Token API returns valid token
 Agora channel join successful
 Instructor video visible
 Self video preview showing
 Chat messages sending/receiving
 Microphone mute/unmute works
 Camera on/off works
 Hand raise updates Firebase
 Leave button exits cleanly
8.2 Debug Logs
Add logging to track issues:

Log.d("LiveClass", "Token received: ${tokenResponse.token}")
Log.d("LiveClass", "Joined channel: $channelName")
Log.d("LiveClass", "Remote user joined: $uid")
Common Issues & Solutions
Issue	Solution
"Permission Denied"	Request CAMERA and RECORD_AUDIO permissions
"Failed to join channel"	Check token API and network connection
"No video showing"	Ensure SurfaceView is added to layout
"Black screen"	Check camera permission and Agora initialization
"Token expired"	Tokens expire after 24h, request new token
Production Checklist
 Add proper error handling for network failures
 Implement reconnection logic
 Add loading/buffering states
 Handle background/foreground transitions
 Optimize video quality based on network
 Add analytics/crash reporting
 Test on different Android versions
 Test with poor network conditions
Ready to Implement! 🚀

Students can now join live classes from your Android app with full video, audio, and chat functionality!