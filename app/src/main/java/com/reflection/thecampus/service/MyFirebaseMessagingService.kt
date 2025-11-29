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

        // Check if message contains a notification payload.
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
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
