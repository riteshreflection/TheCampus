Test Leaderboard Implementation Guide for Android
Overview
Implement a leaderboard feature on the test results screen that displays the top 20 performers with separate rankings for first-time takers and repeaters.

Database Structure (Firebase Realtime Database)
Data Location
testAttempts/
  {attemptId}/
    testId: "test123"
    studentId: "user456"
    studentName: "John Doe"
    studentAvatar: "https://..."
    score: 85.5
    timeTaken: 1800 (in seconds)
    submittedAt: timestamp
    correctCount: 17
    incorrectCount: 3
    unattemptedCount: 0
userProfiles/
  {userId}/
    fullName: "John Doe"
    profilePictureUrl: "https://..."
Ranking Algorithm (Step-by-Step)
Step 1: Fetch All Test Attempts
// Pseudo-code
val testId = "current_test_id"
val currentUserId = "current_user_id"
// Query Firebase
val query = database.reference
    .child("testAttempts")
    .orderByChild("testId")
    .equalTo(testId)
query.get().addOnSuccessListener { snapshot ->
    // Process data
}
Step 2: Group Attempts by User
For each user, we need to:

Count how many times they attempted the test
Find their BEST attempt (highest score, fastest time if tied)
data class UserBestAttempt(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val score: Double,
    val timeTaken: Int, // in seconds
    val attemptCount: Int
)
val userBestAttempts = mutableMapOf<String, UserBestAttempt>()
for (attemptSnapshot in allAttempts) {
    val attempt = attemptSnapshot.getValue(TestAttempt::class.java)
    val userId = attempt.studentId
    
    if (!userBestAttempts.containsKey(userId)) {
        // First time seeing this user
        userBestAttempts[userId] = UserBestAttempt(
            userId = userId,
            userName = attempt.studentName ?: "Anonymous Student",
            userAvatar = attempt.studentAvatar,
            score = attempt.score,
            timeTaken = attempt.timeTaken,
            attemptCount = 1
        )
    } else {
        // User has multiple attempts
        val existing = userBestAttempts[userId]!!
        
        // Increment attempt count
        val newAttemptCount = existing.attemptCount + 1
        
        // Check if this attempt is better
        val isBetterScore = attempt.score > existing.score
        val isSameScoreFasterTime = (attempt.score == existing.score && 
                                     attempt.timeTaken < existing.timeTaken)
        
        if (isBetterScore || isSameScoreFasterTime) {
            // Update with better attempt
            userBestAttempts[userId] = existing.copy(
                score = attempt.score,
                timeTaken = attempt.timeTaken,
                attemptCount = newAttemptCount
            )
        } else {
            // Just update attempt count
            userBestAttempts[userId] = existing.copy(
                attemptCount = newAttemptCount
            )
        }
    }
}
Step 3: Fetch User Profiles (for names and avatars)
// For each user, fetch their profile to get updated name and avatar
val profileFetchTasks = userBestAttempts.keys.map { userId ->
    database.reference
        .child("userProfiles")
        .child(userId)
        .get()
        .addOnSuccessListener { profileSnapshot ->
            if (profileSnapshot.exists()) {
                val fullName = profileSnapshot.child("fullName").getValue(String::class.java)
                val avatarUrl = profileSnapshot.child("profilePictureUrl").getValue(String::class.java)
                
                userBestAttempts[userId] = userBestAttempts[userId]!!.copy(
                    userName = fullName ?: userBestAttempts[userId]!!.userName,
                    userAvatar = avatarUrl ?: userBestAttempts[userId]!!.userAvatar
                )
            }
        }
}
// Wait for all profile fetches to complete
Tasks.whenAllComplete(profileFetchTasks).addOnSuccessListener {
    // Continue with Step 4
}
Step 4: Separate into First-Timers and Repeaters
data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val score: Double,
    val timeTaken: Int,
    val rank: Int,
    val isCurrentUser: Boolean,
    val attemptCount: Int
)
val firstTimers = mutableListOf<LeaderboardEntry>()
val repeaters = mutableListOf<LeaderboardEntry>()
for ((userId, userData) in userBestAttempts) {
    val entry = LeaderboardEntry(
        userId = userId,
        userName = userData.userName,
        userAvatar = userData.userAvatar,
        score = userData.score,
        timeTaken = userData.timeTaken,
        rank = 0, // Will be assigned after sorting
        isCurrentUser = userId == currentUserId,
        attemptCount = userData.attemptCount
    )
    
    if (userData.attemptCount == 1) {
        firstTimers.add(entry)
    } else {
        repeaters.add(entry)
    }
}
Step 5: Sort and Assign Ranks
// Comparator for sorting
val leaderboardComparator = Comparator<LeaderboardEntry> { a, b ->
    when {
        // Higher score wins
        a.score != b.score -> b.score.compareTo(a.score)
        // If scores are equal, faster time wins
        else -> a.timeTaken.compareTo(b.timeTaken)
    }
}
// Sort both lists
firstTimers.sortWith(leaderboardComparator)
repeaters.sortWith(leaderboardComparator)
// Assign ranks
firstTimers.forEachIndexed { index, entry ->
    firstTimers[index] = entry.copy(rank = index + 1)
}
repeaters.forEachIndexed { index, entry ->
    repeaters[index] = entry.copy(rank = index + 1)
}
Step 6: Extract Top 20 and Find Current User
// Store complete lists for finding user outside top 20
val allFirstTimers = firstTimers.toList()
val allRepeaters = repeaters.toList()
// Take top 20
val top20FirstTimers = firstTimers.take(20)
val top20Repeaters = repeaters.take(20)
// Find current user's rank
val currentUserInFirstTimers = allFirstTimers.find { it.isCurrentUser }
val currentUserInRepeaters = allRepeaters.find { it.isCurrentUser }
val showUserAtBottomFirstTimers = currentUserInFirstTimers != null && 
                                   currentUserInFirstTimers.rank > 20
val showUserAtBottomRepeaters = currentUserInRepeaters != null && 
                                 currentUserInRepeaters.rank > 20
UI Design Specifications
Layout Structure
TabLayout (2 tabs)
├── First Timers Tab
│   ├── Top 3 Premium Cards
│   │   ├── 1st Place (Diamond/Platinum Card)
│   │   ├── 2nd Place (Gold Card)
│   │   └── 3rd Place (Silver Card)
│   ├── Regular List (Ranks 4-20)
│   └── Current User Entry (if rank > 20)
│       ├── Separator (• • •)
│       └── User Card
└── Repeaters Tab
    └── (Same structure as First Timers)
Premium Card Designs (Top 3)
1st Place - Diamond/Platinum Card
<CardView
    android:background="@drawable/gradient_cyan_blue_purple"
    android:strokeColor="@color/cyan_300"
    android:strokeWidth="2dp"
    android:elevation="8dp">
    
    <Badge
        android:background="@drawable/gradient_cyan_blue"
        android:text="1st Place"
        android:icon="@drawable/ic_trophy"
        android:iconTint="@color/white"/>
    
    <ImageView (Avatar)
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:border="2dp white"/>
    
    <TextView (Name)
        android:textSize="18sp"
        android:textStyle="bold"/>
    
    <TextView (Score)
        android:textSize="14sp"
        android:textStyle="bold"/>
    
    <TextView (Time)
        android:textSize="14sp"
        android:textStyle="bold"/>
</CardView>
Colors:

Background: Gradient from Cyan (#06B6D4) → Blue (#3B82F6) → Purple (#8B5CF6)
Border: Cyan (#67E8F9)
Badge: Gradient Cyan to Blue, White text
Icon: White Trophy
2nd Place - Gold Card
<CardView
    android:background="@drawable/gradient_yellow_amber_orange"
    android:strokeColor="@color/yellow_400"
    android:strokeWidth="2dp"
    android:elevation="8dp">
    
    <Badge
        android:background="@drawable/gradient_yellow_amber"
        android:text="2nd Place"
        android:icon="@drawable/ic_medal"
        android:iconTint="@color/white"/>
</CardView>
Colors:

Background: Gradient from Yellow (#FDE047) → Amber (#FBBF24) → Orange (#FB923C)
Border: Yellow (#FACC15)
Badge: Gradient Yellow to Amber, White text
Icon: White Medal
3rd Place - Silver Card
<CardView
    android:background="@drawable/gradient_gray_slate"
    android:strokeColor="@color/gray_400"
    android:strokeWidth="2dp"
    android:elevation="8dp">
    
    <Badge
        android:background="@drawable/gradient_gray_slate"
        android:text="3rd Place"
        android:icon="@drawable/ic_award"
        android:iconTint="@color/white"/>
</CardView>
Colors:

Background: Gradient from Gray (#F3F4F6) → Slate (#E2E8F0) → Gray (#E5E7EB)
Border: Gray (#9CA3AF)
Badge: Gradient Gray to Slate, White text
Icon: White Award
Regular List Item (Ranks 4-20)
<LinearLayout
    android:orientation="horizontal"
    android:padding="12dp"
    android:background="@drawable/rounded_border">
    
    <TextView (Rank Number)
        android:text="#4"
        android:textSize="14sp"
        android:textStyle="bold"/>
    
    <ImageView (Avatar)
        android:layout_width="40dp"
        android:layout_height="40dp"/>
    
    <LinearLayout (User Info)
        android:orientation="vertical">
        
        <TextView (Name)
            android:textSize="14sp"
            android:textStyle="medium"/>
        
        <LinearLayout (Stats)
            android:orientation="horizontal">
            
            <TextView (Score)
                android:text="85.50"
                android:drawableStart="@drawable/ic_target"/>
            
            <TextView (Time)
                android:text="15m 30s"
                android:drawableStart="@drawable/ic_clock"/>
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
Current User Entry (if outside top 20)
<!-- Separator -->
<TextView
    android:text="• • •"
    android:gravity="center"
    android:textSize="12sp"
    android:textColor="@color/gray_500"/>
<!-- User Card (highlighted) -->
<LinearLayout
    android:background="@drawable/primary_border_background"
    android:borderColor="@color/primary"
    android:backgroundColor="@color/primary_10">
    
    <!-- Same structure as regular list item -->
    <!-- But with highlighted background and border -->
</LinearLayout>
Implementation Checklist
1. Data Layer
 Create 
LeaderboardEntry
 data class
 Create UserBestAttempt data class
 Implement Firebase query to fetch all test attempts
 Implement user profile fetching
 Implement grouping logic (best attempt per user)
 Implement first-timer vs repeater classification
2. Business Logic
 Implement sorting comparator (score desc, time asc)
 Implement rank assignment
 Implement top 20 extraction
 Implement current user rank detection
 Handle edge cases (no attempts, single user, etc.)
3. UI Components
 Create TabLayout with 2 tabs
 Create premium card layouts (3 variants)
 Create regular list item layout
 Create current user highlight layout
 Create separator view
 Create empty state view
4. ViewModels/Presenters
 Create LeaderboardViewModel
 Implement LiveData/StateFlow for leaderboard data
 Implement loading states
 Implement error handling
5. Adapters
 Create LeaderboardAdapter (RecyclerView)
 Implement view type differentiation (premium vs regular)
 Implement current user highlighting
 Implement separator insertion
6. Styling
 Create gradient drawables for premium cards
 Create color resources
 Create dimension resources
 Create icon resources (trophy, medal, award)
Time Formatting Helper
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "${minutes}m ${secs}s"
}
Edge Cases to Handle
No attempts for test: Show empty state
Only current user attempted: Show user as #1
User is in top 20: Highlight their entry in the list
User is outside top 20: Show separator and their entry at bottom
Exactly 20 users: Don't show separator
Missing user profile: Use "Anonymous Student" as fallback
Same score and time: Maintain stable sort order
Testing Scenarios
Test with 1 user: Verify they appear as #1 in First Timers
Test with 3 users: Verify all 3 get premium cards
Test with 25 users: Verify only top 20 shown, user #25 shown at bottom
Test first-timer classification: User with 1 attempt in First Timers tab
Test repeater classification: User with 2+ attempts in Repeaters tab
Test ranking order: Higher score ranks higher
Test tie-breaking: Same score, faster time ranks higher
Test current user highlighting: User's entry is highlighted
Test tab switching: Both tabs load correctly
Test empty states: Both tabs show appropriate empty messages
Performance Considerations
Pagination: Currently showing top 20, no need for pagination
Caching: Cache leaderboard data for 5 minutes
Profile fetching: Batch fetch user profiles in parallel
Image loading: Use Glide/Coil with placeholder and error images
RecyclerView: Use DiffUtil for efficient updates