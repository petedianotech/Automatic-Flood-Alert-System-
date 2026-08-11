package com.example.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FloodMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FloodMessagingService", "FCM Device Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FloodMessagingService", "Received FCM push payload: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: "🚨 CRITICAL FLOOD ALERT"
        val body = remoteMessage.notification?.body ?: "RED LED status detected. Seek high ground immediately!"
        val waterLevelStr = remoteMessage.data["waterLevelMeters"] ?: "4.8"
        val waterLevel = waterLevelStr.toDoubleOrNull() ?: 4.8
        val location = remoteMessage.data["location"] ?: "River Valley Station #01"

        // Fire a highly visible, system-wide heads-up notification with sound/vibration
        NotificationHelper.triggerEmergencyFloodAlert(
            context = applicationContext,
            waterLevelMeters = waterLevel,
            location = location
        )
    }
}
