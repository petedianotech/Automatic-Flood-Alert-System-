package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DetectionLog
import com.example.data.EmergencyContact
import com.example.data.EvacuationShelter
import com.example.data.LedState
import com.example.data.WaterMonitorRepository
import com.example.engine.OpenCvAnalysisResult
import com.example.engine.OpenCvColorDetector
import com.example.service.FirebaseManager
import com.example.service.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isVoiceListening: Boolean = false,
    val voiceStatusText: String = "Voice Command Dispatch Active • Speak or Tap Command",
    val lastRecognizedSpeech: String = "Tap microphone or say 'RED ALERT'",
    val isMotionSensorArmed: Boolean = false,
    val isMotionDetected: Boolean = false,
    val motionSensorStatusText: String = "Motion Alarm Disarmed • Tap to Arm Guard",
    val motionSensitivity: Float = 3.5f,
    val lastAccelerationDelta: Float = 0f,
    val activeSimulationState: LedState = LedState.GREEN,
    val latestAnalysisResult: OpenCvAnalysisResult? = null,
    val systemHealth: String = "Voice & Motion Guard Node #01 Online • Sensors Active • Battery: 98%",
    val lastCaptureTimeText: String = "Just now"
)

data class VillagerUiState(
    val isAlarmActive: Boolean = false,
    val selectedShelter: EvacuationShelter? = null,
    val checklistItems: Map<String, Boolean> = mapOf(
        "Important Identification & Medical Documents" to true,
        "Emergency Bottled Water & Non-perishable Food" to true,
        "Flashlight & Spare Batteries" to false,
        "First Aid Kit & Prescription Medicines" to false,
        "Emergency Whistle & Power Bank" to false
    )
)

class WaterMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WaterMonitorRepository

    val allLogs: StateFlow<List<DetectionLog>>
    val latestLog: StateFlow<DetectionLog?>
    val emergencyContacts: StateFlow<List<EmergencyContact>>
    val evacuationShelters: StateFlow<List<EvacuationShelter>>
    val firestoreLogs: StateFlow<List<DetectionLog>>

    private val _adminState = MutableStateFlow(AdminUiState())
    val adminState: StateFlow<AdminUiState> = _adminState.asStateFlow()

    private val _villagerState = MutableStateFlow(VillagerUiState())
    val villagerState: StateFlow<VillagerUiState> = _villagerState.asStateFlow()

    init {
        // Initialize offline Firestore and FCM subscription
        FirebaseManager.initialize(application)

        val database = AppDatabase.getDatabase(application)
        repository = WaterMonitorRepository(database.detectionDao())

        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        latestLog = repository.latestLog.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        emergencyContacts = repository.emergencyContacts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        evacuationShelters = repository.evacuationShelters.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Sync Firestore real-time snapshot flow
        firestoreLogs = FirebaseManager.getFirestoreLogsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Perform initial analysis state
        performAnalysis(forcedState = LedState.GREEN, triggerType = "INITIALIZATION")
    }

    fun startVoiceListening() {
        _adminState.update {
            it.copy(
                isVoiceListening = true,
                voiceStatusText = "🎙️ Listening for voice command... Speak now!"
            )
        }
    }

    fun stopVoiceListening() {
        _adminState.update {
            it.copy(
                isVoiceListening = false,
                voiceStatusText = "Voice listener paused • Tap mic to speak"
            )
        }
    }

    fun processVoiceCommand(spokenText: String) {
        val cleanText = spokenText.lowercase(java.util.Locale.getDefault()).trim()
        _adminState.update { it.copy(lastRecognizedSpeech = spokenText) }

        when {
            cleanText.contains("red") || cleanText.contains("evacuate") || cleanText.contains("danger") || cleanText.contains("flood") || cleanText.contains("emergency") || cleanText.contains("run") -> {
                _adminState.update {
                    it.copy(
                        voiceStatusText = "Voice Match: 'RED ALERT' -> Emergency Siren & Evacuation Broadcast Triggered!",
                        activeSimulationState = LedState.RED
                    )
                }
                performAnalysis(forcedState = LedState.RED, triggerType = "VOICE_COMMAND")
            }
            cleanText.contains("blue") || cleanText.contains("warning") || cleanText.contains("rising") || cleanText.contains("caution") -> {
                _adminState.update {
                    it.copy(
                        voiceStatusText = "Voice Match: 'BLUE WARNING' -> Water Rising Flood Warning Broadcasted!",
                        activeSimulationState = LedState.BLUE
                    )
                }
                performAnalysis(forcedState = LedState.BLUE, triggerType = "VOICE_COMMAND")
            }
            cleanText.contains("green") || cleanText.contains("normal") || cleanText.contains("safe") || cleanText.contains("clear") || cleanText.contains("reset") || cleanText.contains("okay") -> {
                _adminState.update {
                    it.copy(
                        voiceStatusText = "Voice Match: 'GREEN NORMAL' -> System Reset to Normal Safe Status",
                        activeSimulationState = LedState.GREEN
                    )
                }
                performAnalysis(forcedState = LedState.GREEN, triggerType = "VOICE_COMMAND")
            }
            else -> {
                _adminState.update {
                    it.copy(
                        voiceStatusText = "Unrecognized Speech: \"$spokenText\". Say 'RED ALERT', 'BLUE WARNING', or 'GREEN NORMAL'."
                    )
                }
            }
        }
    }

    fun setSimulationLedState(state: LedState) {
        _adminState.update { it.copy(activeSimulationState = state) }
        val triggerName = when (state) {
            LedState.RED -> "MANUAL_RED_ALERT"
            LedState.BLUE -> "MANUAL_BLUE_WARNING"
            LedState.GREEN -> "MANUAL_GREEN_NORMAL"
            LedState.UNKNOWN -> "MANUAL_TRIGGER"
        }
        performAnalysis(forcedState = state, triggerType = triggerName)
    }

    fun toggleMotionSensor(armed: Boolean) {
        _adminState.update {
            it.copy(
                isMotionSensorArmed = armed,
                isMotionDetected = if (!armed) false else it.isMotionDetected,
                motionSensorStatusText = if (armed) "🛡️ Motion Guard ARMED • Listening for movement/vibration" else "Motion Guard Disarmed • Tap to Arm"
            )
        }
    }

    fun updateMotionSensitivity(sensitivity: Float) {
        _adminState.update { it.copy(motionSensitivity = sensitivity) }
    }

    fun processAccelerometerValues(x: Float, y: Float, z: Float) {
        val currentState = _adminState.value
        if (!currentState.isMotionSensorArmed) return

        // Standard gravity is approx 9.81 m/s²
        val totalAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = kotlin.math.abs(totalAcceleration - 9.81f)

        _adminState.update { it.copy(lastAccelerationDelta = delta) }

        if (delta > currentState.motionSensitivity && !currentState.isMotionDetected) {
            triggerMotionAlarm("PHYSICAL_MOVEMENT_SENSOR")
        }
    }

    fun triggerMotionAlarm(reason: String = "MOTION_SENSOR") {
        _adminState.update {
            it.copy(
                isMotionDetected = true,
                motionSensorStatusText = "🚨 MOTION / TAMPER ALARM TRIGGERED! Device moved!",
                activeSimulationState = LedState.RED
            )
        }
        _villagerState.update { it.copy(isAlarmActive = true) }
        performAnalysis(forcedState = LedState.RED, triggerType = reason)
    }

    fun resetMotionAlarm() {
        _adminState.update {
            it.copy(
                isMotionDetected = false,
                motionSensorStatusText = if (it.isMotionSensorArmed) "🛡️ Motion Guard ARMED • Monitoring active" else "Motion Guard Disarmed"
            )
        }
        _villagerState.update { it.copy(isAlarmActive = false) }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(onLocationResult: (Double, Double, String) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        
        val hasFine = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val name = getStationNameForCoords(location.latitude, location.longitude)
                        onLocationResult(location.latitude, location.longitude, name)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                val name = getStationNameForCoords(lastLoc.latitude, lastLoc.longitude)
                                onLocationResult(lastLoc.latitude, lastLoc.longitude, name)
                            } else {
                                onLocationResult(14.5995, 120.9842, "River Station Alpha")
                            }
                        }.addOnFailureListener {
                            onLocationResult(14.5995, 120.9842, "River Station Alpha")
                        }
                    }
                }
                .addOnFailureListener {
                    onLocationResult(14.5995, 120.9842, "River Station Alpha")
                }
        } else {
            onLocationResult(14.5995, 120.9842, "River Station Alpha")
        }
    }

    private fun getStationNameForCoords(lat: Double, lng: Double): String {
        return if (lat == 14.5995 && lng == 120.9842) {
            "River Station Alpha"
        } else {
            val nodeNum = (Math.abs(lat + lng) * 100).toInt() % 4 + 1
            "Flood Detection Node #$nodeNum"
        }
    }

    private var lastLoggedState: LedState? = null

    private fun performAnalysis(
        forcedState: LedState? = null,
        customBitmap: Bitmap? = null,
        triggerType: String = "MANUAL"
    ) {
        val targetState = forcedState ?: _adminState.value.activeSimulationState
        val frameBitmap = customBitmap ?: OpenCvColorDetector.createSimulatedFrame(targetState)
        val result = OpenCvColorDetector.analyzeBitmap(frameBitmap, forcedState = forcedState)

        _adminState.update {
            it.copy(
                latestAnalysisResult = result,
                lastCaptureTimeText = formatCurrentTime()
            )
        }

        // Throttle continuous LIVE_STREAM logging
        if (triggerType == "LIVE_STREAM" && result.detectedState == lastLoggedState) {
            return
        }
        
        lastLoggedState = result.detectedState

        fetchCurrentLocation { latitude, longitude, locationName ->
            // Save detection to Room database
            viewModelScope.launch {
                val statusMessage = when (result.detectedState) {
                    LedState.GREEN -> "Normal status (GREEN LED). Everything is okay at $locationName."
                    LedState.BLUE -> "WARNING alert active (BLUE LED). Water level rising near $locationName. Prepare emergency supplies."
                    LedState.RED -> "CRITICAL FLOOD DANGER (RED LED). EVACUATE IMMEDIATELY at $locationName! PEOPLE MUST RUN TO SAFETY!"
                    LedState.UNKNOWN -> "Optical camera sensor re-calibrating at $locationName."
                }

                val newLog = DetectionLog(
                    timestamp = System.currentTimeMillis(),
                    ledState = result.detectedState.name,
                    waterLevelMeters = 0.0,
                    confidence = result.confidence,
                    hsvDetails = "H: ${result.detectedHue.toInt()}° S: ${(result.detectedSaturation * 100).toInt()}% V: ${(result.detectedBrightness * 100).toInt()}%",
                    triggerType = triggerType,
                    detectedColorHex = result.colorHex,
                    statusSummary = statusMessage,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName
                )

                repository.saveDetectionLog(newLog)

                // Save and synchronize to Firestore (supported fully offline via Firestore caching settings)
                FirebaseManager.saveDetectionLog(newLog)

                // Trigger alerts and audio siren toggles dynamically for BLUE and RED states only
                if (result.detectedState == LedState.BLUE || result.detectedState == LedState.RED) {
                    if (result.detectedState == LedState.RED) {
                        _villagerState.update { it.copy(isAlarmActive = true) }
                    }
                    
                    // Trigger dynamic localized notification on device for risk detection
                    NotificationHelper.triggerEmergencyFloodAlert(
                        context = getApplication(),
                        location = locationName,
                        isCritical = (result.detectedState == LedState.RED)
                    )
                }
            }
        }
    }

    fun toggleAlarmSiren() {
        _villagerState.update { it.copy(isAlarmActive = !it.isAlarmActive) }
    }

    fun toggleChecklistItem(itemKey: String) {
        _villagerState.update { state ->
            val updated = state.checklistItems.toMutableMap()
            updated[itemKey] = !(updated[itemKey] ?: false)
            state.copy(checklistItems = updated)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun sendBroadcastNotification(title: String, body: String) {
        FirebaseManager.sendBroadcastNotification(title, body)
    }

    private fun formatCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
