# Quick Start Guide: Debugging Course Display Issues

## What We've Done

Added comprehensive logging to trace why courses aren't showing in:
1. **CourseDetailActivity** - Individual course details page
2. **MyCoursesFragment** - User's enrolled courses list
3. **DiscoverFragment** - All available courses list

## How to Use Right Now

### Step 1: Build and Run
```bash
# Clean and rebuild the app
./gradlew clean assembleDebug

# Or click Run in Android Studio
```

### Step 2: Open Logcat
1. In Android Studio: **View → Tool Windows → Logcat** (or press Alt+6)
2. Set filter to show only `Timber` tag
3. Or search for these keywords:
   - `getCourse`
   - `enrolledCourses`
   - `observer triggered`

### Step 3: Test Each Screen

#### Test 1: Course Detail Page
1. Open any course from Discover tab
2. **Look for in Logcat:**
   - `CourseDetailActivity: Final courseId: '{some-id}'`
   - `✓ Course loaded successfully: {course-name}`
   - `✓ Course data received: {course-name}`

3. **If you see issues:**
   - `Final courseId: ''` → Course ID not being passed ❌
   - `Firebase snapshot exists: false` → Course doesn't exist in Firebase ❌
   - `⚠ Course is null!` → Data parsing failed ❌

#### Test 2: My Courses Tab
1. Login and open "My Courses" tab
2. **Look for in Logcat:**
   - `Current user: {userId}`
   - `Enrolled course IDs: {id1}, {id2}...`
   - `📥 Enrolled courses Flow emitted: X courses`
   - `✓ Adapter set with X courses`

3. **If you see issues:**
   - `No authenticated user` → Not logged in ❌
   - `childrenCount: 0` → No enrolled courses ❌
   - `⚠ Failed to fetch course` → Courses missing in Firebase ❌

#### Test 3: Discover/Explore Tab
1. Open Discover/Explore tab
2. **Look for in Logcat:**
   - `Starting Flow collection for all courses...`
   - `📥 All courses Flow emitted: X courses`
   - `✓ Adapter updated successfully`

3. **If you see issues:**
   - `⚠ No courses found in Firebase` → Database empty ❌
   - `Cannot update adapter` → No data ❌

## Quick Fixes

### Fix 1: No Courses in Firebase
**Add sample course via Firebase Console:**
```
Firebase Console → Realtime Database → /courses → Add Child

{
  "basicInfo": {
    "name": "Test Course",
    "description": "A test course",
    "level": "Beginner",
    "type": "Online"
  },
  "pricing": {
    "price": 999,
    "discount": 10,
    "thumbnailUrl": "https://via.placeholder.com/400x200"
  },
  "instructorIds": [],
  "linkedTests": [],
  "linkedClasses": [],
  "status": "active",
  "createdAt": 1700000000000
}
```

### Fix 2: No Enrolled Courses
**Enroll user manually:**
```
Firebase Console → Realtime Database → /users/{userId}/enrolledCourses

Add: {courseId}: true
```

### Fix 3: User Not Authenticated
1. Make sure you're logged in
2. Check: Settings → Logout → Login again

### Fix 4: Clear App Data
```
Device Settings → Apps → TheCampus → Storage → Clear Data
```
Then re-login

## Understanding Log Symbols

- ✓ = Success! Everything worked
- ⚠ = Warning - something might be wrong
- ❌ = Error - critical issue
- 📥 = Data received from Firebase

## Common Log Patterns

### ✅ GOOD - Course Loading Successfully
```
getCourse() called with courseId: -ABC123
✓ Course -ABC123 loaded from cache: Test Course
✓ Course loaded successfully: Test Course
Course LiveData observer triggered
✓ Course data received: Test Course
```

### ❌ BAD - Course Not Found
```
getCourse() called with courseId: -ABC123
Firebase snapshot exists: false
⚠ Course -ABC123 does not exist in Firebase
⚠ Course is null! Failed to load course
```

### ❌ BAD - No Enrolled Courses
```
Enrolled courses snapshot received - exists: false, childrenCount: 0
No enrolled courses found, emitting empty list
MyCoursesFragment: Received 0 enrolled courses
Showing empty state (no enrolled courses)
```

### ❌ BAD - Data Parsing Failed
```
Firebase snapshot exists: true, hasChildren: true
Parsed course from Firebase: null
⚠ Failed to parse course -ABC123 from Firebase snapshot
```

## Next Steps

1. **Run the app and check Logcat** (do this first!)
2. **Identify which pattern matches your logs**
3. **Apply the corresponding fix**
4. **Share logs if still stuck** - Screenshot or copy the relevant log lines

## Where to Find Logs

After running the app:
1. Android Studio → Logcat tab (bottom)
2. Filter by "Timber" tag
3. Look for the patterns above
4. Screenshot or copy relevant lines

## Need More Help?

See detailed guides:
- **DEBUGGING_GUIDE.md** - Comprehensive debugging steps
- **IMPLEMENTATION_SUMMARY.md** - What we changed and why

## Firebase Structure Check

Your Firebase should look like this:
```
/courses
  /{courseId}
    /basicInfo
      name: "Course Name"
      description: "..."
    /pricing
      price: 999
      discount: 10
      thumbnailUrl: "https://..."
    /instructorIds: []
    /linkedTests: []
    /linkedClasses: []

/users
  /{userId}
    /enrolledCourses
      /{courseId}: true
```

If structure is different, that's likely the issue!

## Test Checklist

- [ ] App builds and runs without errors
- [ ] Logcat is open and showing Timber logs
- [ ] Clicked a course from Discover tab
- [ ] Checked logs for courseId
- [ ] Verified course exists in Firebase
- [ ] Opened My Courses tab
- [ ] Checked if user is authenticated
- [ ] Verified enrolled courses in Firebase
- [ ] Checked for any ❌ or ⚠ symbols in logs
- [ ] Tried one of the Quick Fixes above

## Success = Seeing These Logs

When everything works, you should see:
- ✓ symbols throughout
- Course names appearing in logs
- "Adapter set with X courses" messages
- No ❌ or ⚠ symbols

That means data is flowing correctly from Firebase → App → Screen!

