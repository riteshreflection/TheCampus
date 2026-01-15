package com.reflection.thecampus.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reflection.thecampus.MainActivity
import com.reflection.thecampus.R
import com.reflection.thecampus.TheCampusApplication
import com.reflection.thecampus.utils.FCMManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // If user is logged in, update the token on the server
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FCMManager.saveToken(this, currentUser.uid)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        
        // Handle different notification types
        when (data["type"]) {
            "course_chat" -> {
                val courseId = data["courseId"] ?: return
                val courseName = data["courseName"] ?: "Course Chat"
                showChatNotification(courseId, courseName, remoteMessage.notification)
            }
            else -> {
                // Default notification handling
                remoteMessage.notification?.let {
                    showNotification(it.title, it.body)
                }
                
                // Also check data payload if needed
                if (remoteMessage.data.isNotEmpty()) {
                     val title = remoteMessage.data["title"]
                     val body = remoteMessage.data["body"]
                     if (title != null && body != null) {
                         showNotification(title, body)
                     }
                }
            }
        }
    }
    
    private fun showChatNotification(
        courseId: String,
        courseName: String,
        notification: RemoteMessage.Notification?
    ) {
        // Create intent to open CourseChatActivity
        val intent = Intent(this, com.reflection.thecampus.ui.chat.CourseChatActivity::class.java).apply {
            putExtra("COURSE_ID", courseId)
            putExtra("COURSE_NAME", courseName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            courseId.hashCode(), // Unique request code per course
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = TheCampusApplication.CHANNEL_ID_GENERAL
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_chat_bubble)
            .setContentTitle(notification?.title ?: courseName)
            .setContentText(notification?.body ?: "New message")
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setGroup("course_chat_$courseId") // Group notifications by course
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(courseId.hashCode(), notificationBuilder.build())
    }

    private fun showNotification(title: String?, body: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = TheCampusApplication.CHANNEL_ID_GENERAL
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // TODO: Use a proper notification icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
