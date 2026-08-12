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
    val isSchedulerEnabled: Boolean = true,
    val schedulerIntervalSeconds: Int = 15,
    val countdownSeconds: Int = 15,
    val isAnalyzing: Boolean = false,
    val isCameraOpen: Boolean = false,
    val cameraStatusText: String = "Automated System Active • Camera Closed (Standby)",
    val activeSimulationState: LedState = LedState.GREEN,
    val latestAnalysisResult: OpenCvAnalysisResult? = null,
    val systemHealth: String = "Flood Node #01 Online • Signal: 98% • Battery: 94%",
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

    private var schedulerJob: Job? = null

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

        // Perform initial frame generation and analysis
        performAnalysis(forcedState = LedState.GREEN, triggerType = "INITIALIZATION")

        // Auto-start camera schedule automatically on boot
        toggleCameraScheduler(true)
    }

    fun openCamera() {
        _adminState.update {
            it.copy(
                isCameraOpen = true,
                cameraStatusText = "Camera Open • Live Optical Feed Active"
            )
        }
    }

    fun stopCamera() {
        _adminState.update {
            it.copy(
                isCameraOpen = false,
                cameraStatusText = "Camera Closed • Standby Mode"
            )
        }
    }

    fun toggleCameraScheduler(enabled: Boolean) {
        _adminState.update {
            it.copy(
                isSchedulerEnabled = enabled,
                isCameraOpen = false,
                cameraStatusText = if (enabled) "Camera Scheduler Active" else "Scheduler Paused • Manual Mode"
            )
        }
        if (enabled) {
            startSchedulerTimer()
        } else {
            schedulerJob?.cancel()
            schedulerJob = null
        }
    }

    fun updateSchedulerInterval(seconds: Int) {
        val validSeconds = seconds.coerceAtLeast(3)
        _adminState.update { it.copy(schedulerIntervalSeconds = validSeconds, countdownSeconds = validSeconds) }
        if (_adminState.value.isSchedulerEnabled) {
            startSchedulerTimer()
        }
    }

    private fun startSchedulerTimer() {
        schedulerJob?.cancel()
        schedulerJob = viewModelScope.launch {
            while (_adminState.value.isSchedulerEnabled) {
                var remaining = _adminState.value.schedulerIntervalSeconds
                _adminState.update {
                    it.copy(
                        countdownSeconds = remaining,
                        isCameraOpen = false,
                        cameraStatusText = "Camera Closed (Standby) • Next scan in ${remaining}s"
                    )
                }
                while (remaining > 0 && _adminState.value.isSchedulerEnabled) {
                    delay(1000)
                    remaining--
                    _adminState.update {
                        it.copy(
                            countdownSeconds = remaining,
                            cameraStatusText = "Camera Closed (Standby) • Next scan in ${remaining}s"
                        )
                    }
                }

                if (_adminState.value.isSchedulerEnabled) {
                    // Open camera shutter for capture
                    _adminState.update {
                        it.copy(
                            isCameraOpen = true,
                            isAnalyzing = true,
                            cameraStatusText = "CAMERA OPEN • Capturing Frame & Analyzing..."
                        )
                    }
                    delay(800) // Shutter exposure simulation

                    performAnalysis(triggerType = "SCHEDULED")

                    // Close camera shutter
                    _adminState.update {
                        it.copy(
                            isCameraOpen = false,
                            isAnalyzing = false,
                            cameraStatusText = "Camera Closed • Detection Logged"
                        )
                    }
                }
            }
        }
    }

    fun captureAndAnalyze(forcedState: LedState? = null, customBitmap: Bitmap? = null) {
        viewModelScope.launch {
            _adminState.update {
                it.copy(
                    isCameraOpen = true,
                    isAnalyzing = true,
                    cameraStatusText = "CAMERA OPEN • Manual Scan Capturing..."
                )
            }
            delay(500)
            performAnalysis(forcedState = forcedState ?: _adminState.value.activeSimulationState, customBitmap = customBitmap, triggerType = "MANUAL")
            _adminState.update {
                it.copy(
                    isCameraOpen = false,
                    isAnalyzing = false,
                    cameraStatusText = "Camera Closed • Manual Scan Saved"
                )
            }
        }
    }

    fun cycleSimulationLedState() {
        val currentState = _adminState.value.activeSimulationState
        val nextState = when (currentState) {
            LedState.GREEN -> LedState.BLUE
            LedState.BLUE -> LedState.RED
            LedState.RED -> LedState.GREEN
            LedState.UNKNOWN -> LedState.GREEN
        }
        _adminState.update { it.copy(activeSimulationState = nextState) }
        captureAndAnalyze(forcedState = nextState)
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
