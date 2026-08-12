package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.DetectionLog
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirebaseManager {

    private var dbInstance: FirebaseFirestore? = null
    private val appStartTime = System.currentTimeMillis()

    fun initialize(context: Context) {
        try {
            // Explicitly initialize FirebaseApp if not already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                                .setSizeBytes(104857600) // 100 MB high performance cache
                                .build()
                        )
                        .build()
                    db.firestoreSettings = settings
                } catch (e: Throwable) {
                    Log.w("FirebaseManager", "Firestore settings already applied or unavailable: ${e.message}")
                }

                dbInstance = db
                Log.d("FirebaseManager", "Firebase Firestore initialized.")

                try {
                    FirebaseMessaging.getInstance().subscribeToTopic("flood_alerts")
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirebaseManager", "Successfully subscribed to FCM topic 'flood_alerts'")
                            }
                        }
                } catch (e: Throwable) {
                    Log.w("FirebaseManager", "FCM messaging topic subscription skipped: ${e.message}")
                }

                // Real-time listener for native custom broadcast notifications
                db.collection("broadcast_notifications")
                    .whereGreaterThan("timestamp", appStartTime)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("FirebaseManager", "Error listening to broadcasts: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            for (change in snapshot.documentChanges) {
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    val doc = change.document
                                    val title = doc.getString("title") ?: "📢 Community Broadcast"
                                    val body = doc.getString("body") ?: "A new message was broadcasted."
                                    NotificationHelper.showCustomNotification(context, title, body)
                                }
                            }
                        }
                    }
            }
        } catch (e: Throwable) {
            Log.e("FirebaseManager", "Firebase initialize error (safe fallback to Room DB): ${e.message}")
        }
    }

    /**
     * Broadcasts a custom message to all app installations.
     * Writes to Firestore with real-time replication for instant client alerts.
     */
    fun sendBroadcastNotification(title: String, body: String) {
        try {
            val db = dbInstance ?: FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "title" to title,
                "body" to body,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("broadcast_notifications")
                .add(data)
                .addOnSuccessListener {
                    Log.d("FirebaseManager", "Broadcast notification sent successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseManager", "Failed to write broadcast notification: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Broadcast send exception: ${e.message}")
        }
    }

    /**
     * Saves a flood alert log to Firestore. Works perfectly offline.
     * Documents will be queued locally and sent to the cloud database when online.
     */
    fun saveDetectionLog(log: DetectionLog) {
        try {
            val db = dbInstance ?: FirebaseFirestore.getInstance()
            val docData = hashMapOf(
                "timestamp" to log.timestamp,
                "ledState" to log.ledState,
                "waterLevelMeters" to log.waterLevelMeters,
                "confidence" to log.confidence,
                "hsvDetails" to log.hsvDetails,
                "triggerType" to log.triggerType,
                "detectedColorHex" to log.detectedColorHex,
                "statusSummary" to log.statusSummary
            )

            db.collection("flood_alert_logs")
                .document(log.timestamp.toString())
                .set(docData)
                .addOnSuccessListener {
                    Log.d("FirebaseManager", "Flood alert synchronized to Firestore successfully.")
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseManager", "Firestore sync queued offline: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Firestore save exception: ${e.message}")
        }
    }

    /**
     * Retrieves flood alert logs from Firestore in real-time.
     * Listens to cloud database updates and updates local flow.
     */
    fun getFirestoreLogsFlow(): Flow<List<DetectionLog>> = callbackFlow {
        try {
            val db = dbInstance ?: if (FirebaseApp.getApps(com.google.firebase.FirebaseApp.getInstance().applicationContext).isNotEmpty()) FirebaseFirestore.getInstance() else null
            if (db == null) {
                trySend(emptyList())
                awaitClose {}
                return@callbackFlow
            }

            val subscription = db.collection("flood_alert_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Firestore listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val logsList = mutableListOf<DetectionLog>()
                        for (doc in snapshot.documents) {
                            try {
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val ledState = doc.getString("ledState") ?: "UNKNOWN"
                                val waterLevelMeters = doc.getDouble("waterLevelMeters") ?: 0.0
                                val confidence = doc.getDouble("confidence") ?: 1.0
                                val hsvDetails = doc.getString("hsvDetails") ?: ""
                                val triggerType = doc.getString("triggerType") ?: "MANUAL"
                                val detectedColorHex = doc.getString("detectedColorHex") ?: "#808080"
                                val statusSummary = doc.getString("statusSummary") ?: ""

                                logsList.add(
                                    DetectionLog(
                                        id = timestamp, // mapping ID
                                        timestamp = timestamp,
                                        ledState = ledState,
                                        waterLevelMeters = waterLevelMeters,
                                        confidence = confidence.toFloat(),
                                        hsvDetails = hsvDetails,
                                        triggerType = triggerType,
                                        detectedColorHex = detectedColorHex,
                                        statusSummary = statusSummary
                                    )
                                )
                            } catch (parseEx: Throwable) {
                                Log.e("FirebaseManager", "Error parsing Firestore log document: ${parseEx.message}")
                            }
                        }
                        trySend(logsList)
                    }
                }

            awaitClose {
                subscription.remove()
            }
        } catch (e: Throwable) {
            Log.e("FirebaseManager", "Firestore callbackFlow exception: ${e.message}")
            trySend(emptyList())
            awaitClose {}
        }
    }
}
