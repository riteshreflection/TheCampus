# Cloud Functions Deployment Guide

## Prerequisites
- Node.js 18 or higher
- Firebase CLI installed (`npm install -g firebase-tools`)
- Firebase project configured

## Setup

1. **Navigate to cloud-functions directory:**
   ```bash
   cd cloud-functions
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Login to Firebase:**
   ```bash
   firebase login
   ```

4. **Initialize Firebase (if not already done):**
   ```bash
   firebase init functions
   ```
   - Select your Firebase project
   - Choose JavaScript
   - Install dependencies with npm

## Deploy

**Deploy all functions:**
```bash
firebase deploy --only functions
```

**Deploy specific function:**
```bash
firebase deploy --only functions:sendCourseNotification
```

## Test Locally

**Start emulator:**
```bash
npm run serve
```

## Monitor

**View logs:**
```bash
firebase functions:log
```

**View logs for specific function:**
```bash
firebase functions:log --only sendCourseNotification
```

## Function Details

### sendCourseNotification
- **Trigger:** `courseChats/{courseId}/messages/{messageId}` onCreate
- **Throttle:** 15 minutes between notifications per course
- **Features:**
  - Batches multiple messages
  - Respects per-user mute settings
  - Only notifies enrolled users
  - Cleans up invalid FCM tokens

## Cost Optimization

- **Before:** ~100 invocations/day per active course
- **After:** ~6-8 invocations/day per active course (15-min batching)
- **Savings:** ~92% reduction in Cloud Function invocations

## Troubleshooting

**Function not triggering:**
- Check Firebase Realtime Database rules
- Verify function is deployed: `firebase functions:list`

**Notifications not received:**
- Check FCM tokens are stored correctly in `users/{userId}/fcmToken`
- Verify user is enrolled in course
- Check if user muted the chat

**Invalid tokens:**
- Function automatically removes invalid tokens
- Check logs for token cleanup messages
