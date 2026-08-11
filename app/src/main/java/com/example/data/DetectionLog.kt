package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LedState {
    GREEN,   // Safe water level
    YELLOW,  // Warning / rising water level
    RED,     // Critical / Evacuate level
    UNKNOWN  // Sensor offline or obstructed
}

@Entity(tableName = "detection_logs")
data class DetectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ledState: String = LedState.GREEN.name,
    val waterLevelMeters: Double = 1.2,
    val confidence: Float = 0.95f,
    val hsvDetails: String = "H: 120° S: 88% V: 92%",
    val triggerType: String = "MANUAL", // "MANUAL" or "SCHEDULED"
    val detectedColorHex: String = "#22C55E",
    val statusSummary: String = "Normal water flow observed"
)
