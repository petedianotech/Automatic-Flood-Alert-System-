package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DetectionLog
import com.example.data.LedState
import com.example.data.Quadruple
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: WaterMonitorViewModel,
    adminState: AdminUiState,
    latestLog: DetectionLog?,
    logs: List<DetectionLog>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val scrollState = rememberScrollState()

    // Register Accelerometer Motion Listener when Motion Sensor Guard is armed
    DisposableEffect(adminState.isMotionSensorArmed) {
        if (adminState.isMotionSensorArmed) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        viewModel.processAccelerometerValues(x, y, z)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        AdminDashboardHeader()

        // Audio Permission Card
        if (!audioPermissionState.status.isGranted) {
            AudioPermissionCard(
                onRequestPermission = { audioPermissionState.launchPermissionRequest() }
            )
        }

        // Voice Command Control Hub
        VoiceCommandSection(
            adminState = adminState,
            isPermissionGranted = audioPermissionState.status.isGranted,
            onVoiceResult = { spokenText -> viewModel.processVoiceCommand(spokenText) },
            onSetSimulationState = { state -> viewModel.setSimulationLedState(state) },
            onRequestPermission = { audioPermissionState.launchPermissionRequest() }
        )

        // Motion & Anti-Theft Sensor Guard
        MotionSensorSection(
            adminState = adminState,
            onToggleArm = { armed -> viewModel.toggleMotionSensor(armed) },
            onUpdateSensitivity = { sensitivity -> viewModel.updateMotionSensitivity(sensitivity) },
            onSimulateMotion = { viewModel.triggerMotionAlarm("SIMULATED_PHYSICAL_MOVEMENT") },
            onResetAlarm = { viewModel.resetMotionAlarm() }
        )

        // Real-Time System Status Card
        RealTimeAnalysisCard(
            adminState = adminState,
            latestLog = latestLog
        )

        // Safety Recommendations
        SafetyRecommendationsCard(
            ledState = adminState.latestAnalysisResult?.detectedState ?: LedState.GREEN
        )
    }
}

@Composable
fun AdminDashboardHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dashboard_header"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin Voice Command Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hands-Free Voice Flood Alert & Emergency Siren System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = StatusGreenBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusGreen)
                    )
                    Text(
                        text = "VOICE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreenText
                    )
                }
            }
        }
    }
}

@Composable
fun AudioPermissionCard(onRequestPermission: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_permission_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Microphone Permission Required",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Microphone access is needed to listen for voice alert commands like 'RED ALERT', 'BLUE WARNING', or 'GREEN NORMAL'.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Microphone Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VoiceCommandSection(
    adminState: AdminUiState,
    isPermissionGranted: Boolean,
    onVoiceResult: (String) -> Unit,
    onSetSimulationState: (LedState) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                onVoiceResult(spokenText)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_command_section_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Voice Command Control Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "SPEECH ENGINE READY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Big Animated Microphone Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing outer aura
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )

                        // Main microphone FAB
                        IconButton(
                            onClick = {
                                if (isPermissionGranted) {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'RED ALERT', 'BLUE WARNING', or 'GREEN NORMAL'")
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        onVoiceResult("Speech Recognition unavailable")
                                    }
                                } else {
                                    onRequestPermission()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("speak_voice_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Tap to speak voice command",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "TAP MICROPHONE TO SPEAK COMMAND",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = adminState.voiceStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Speech Log Display Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LAST RECOGNIZED SPEECH:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "\"${adminState.lastRecognizedSpeech}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            // Voice Command Cheat Sheet & Quick Simulation Chips
            Text(
                text = "Voice Command Cheat Sheet & Instant Simulators:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // RED COMMAND
                CommandGuideCard(
                    color = StatusRed,
                    bgColor = StatusRedBg,
                    title = "🔴 RED ALERT / EVACUATE",
                    phrase = "\"Red Alert\" or \"Evacuate\" or \"Flood Danger\"",
                    effect = "Triggers Emergency Siren & Instant Evacuation Broadcast",
                    onClick = { onSetSimulationState(LedState.RED) },
                    testTag = "quick_sim_red_button"
                )

                // BLUE COMMAND
                CommandGuideCard(
                    color = StatusBlue,
                    bgColor = StatusBlueBg,
                    title = "🔵 BLUE WARNING / WATER RISING",
                    phrase = "\"Blue Warning\" or \"Water Rising\" or \"Caution\"",
                    effect = "Triggers Warning Alert & Preparation Mobile Push",
                    onClick = { onSetSimulationState(LedState.BLUE) },
                    testTag = "quick_sim_blue_button"
                )

                // GREEN COMMAND
                CommandGuideCard(
                    color = StatusGreen,
                    bgColor = StatusGreenBg,
                    title = "🟢 GREEN NORMAL / CLEAR",
                    phrase = "\"Green Normal\" or \"Safe\" or \"Clear\" or \"Reset\"",
                    effect = "Resets Alarm & Sets Village Status to Safe",
                    onClick = { onSetSimulationState(LedState.GREEN) },
                    testTag = "quick_sim_green_button"
                )
            }
        }
    }
}

@Composable
fun CommandGuideCard(
    color: Color,
    bgColor: Color,
    title: String,
    phrase: String,
    effect: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = "Say: $phrase",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.9f)
                )
                Text(
                    text = effect,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RealTimeAnalysisCard(
    adminState: AdminUiState,
    latestLog: DetectionLog?
) {
    val result = adminState.latestAnalysisResult
    val ledState = result?.detectedState ?: LedState.GREEN

    val ledColor = when (ledState) {
        LedState.GREEN -> StatusGreen
        LedState.BLUE -> StatusBlue
        LedState.RED -> StatusRed
        LedState.UNKNOWN -> Color.Gray
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ledPulse"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("real_time_analysis_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current Village Alert System State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (ledState == LedState.RED || ledState == LedState.BLUE) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(ledColor.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ledColor)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ledColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "SYSTEM ALERT: ${ledState.name}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ledColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (ledState) {
                            LedState.GREEN -> "GREEN - Normal (Everything Safe)"
                            LedState.BLUE -> "BLUE - Warning Alert"
                            LedState.RED -> "RED - DANGER EVACUATE NOW!"
                            LedState.UNKNOWN -> "UNKNOWN STATUS"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ledColor
                    )

                    Text(
                        text = latestLog?.statusSummary ?: "Normal status active. All clear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyRecommendationsCard(
    ledState: LedState
) {
    val (statusTitle, statusColor, statusBg, recommendations) = when (ledState) {
        LedState.GREEN -> Quadruple(
            "NORMAL STATUS RECOMMENDATIONS",
            StatusGreen,
            StatusGreenBg,
            listOf(
                "Everything is okay. No immediate flood threat detected.",
                "Voice alert listener active across river monitoring stations.",
                "Keep emergency contacts saved on your mobile device."
            )
        )
        LedState.BLUE -> Quadruple(
            "WARNING STATUS RECOMMENDATIONS",
            StatusBlue,
            StatusBlueBg,
            listOf(
                "WARNING: River water levels rising. Stand by for community updates.",
                "Charge mobile phones, powerbanks, and emergency flashlights.",
                "Move livestock, essential documents, and supplies to high ground.",
                "Prepare disaster response grab-and-go kits."
            )
        )
        LedState.RED -> Quadruple(
            "DANGER STATUS RECOMMENDATIONS",
            StatusRed,
            StatusRedBg,
            listOf(
                "CRITICAL FLOOD DANGER! PEOPLE MUST RUN TO SAFETY!",
                "EVACUATE IMMEDIATELY to designated high-ground village shelters!",
                "Do NOT attempt to walk or drive through flowing water.",
                "Assist children, elderly neighbors, and persons with disabilities."
            )
        )
        LedState.UNKNOWN -> Quadruple(
            "STATUS UNKNOWN RECOMMENDATIONS",
            Color.Gray,
            Color(0xFFF1F5F9),
            listOf(
                "Voice dispatch system re-calibrating.",
                "Verify microphone signal and station connection."
            )
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safety_recommendations_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusBg
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            HorizontalDivider(color = statusColor.copy(alpha = 0.2f))

            recommendations.forEach { recommendation ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (ledState == LedState.RED) FontWeight.Bold else FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun MotionSensorSection(
    adminState: AdminUiState,
    onToggleArm: (Boolean) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onSimulateMotion: () -> Unit,
    onResetAlarm: () -> Unit
) {
    val isArmed = adminState.isMotionSensorArmed
    val isDetected = adminState.isMotionDetected

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("motion_sensor_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDetected) StatusRedBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDetected) StatusRed
                                else if (isArmed) StatusGreen
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDetected) Icons.Default.Vibration else Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Motion & Anti-Theft Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArmed) "Accelerometer Active • Detects Movement" else "Sensor Disarmed • Standby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isArmed,
                    onCheckedChange = onToggleArm,
                    modifier = Modifier.testTag("arm_motion_guard_switch")
                )
            }

            // Alert Banner if Motion Detected
            if (isDetected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusRed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = "🚨 TAMPER / PHONE MOVEMENT DETECTED!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "The device was physically moved or shaken while armed. High-volume emergency alarm is sounding!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onResetAlarm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("silence_motion_alarm_button")
                        ) {
                            Text(
                                text = "🔕 SILENCE & RESET ALARM",
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                        }
                    }
                }
            } else {
                // Live Status Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isArmed) StatusGreenBg else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (isArmed) StatusGreenText else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = adminState.motionSensorStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isArmed) StatusGreenText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isArmed) {
                                Text(
                                    text = "Current Movement Delta: ${String.format("%.2f", adminState.lastAccelerationDelta)} m/s² (Threshold: ${String.format("%.1f", adminState.motionSensitivity)} m/s²)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusGreenText.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Motion Sensitivity Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Motion Sensitivity Threshold:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            adminState.motionSensitivity <= 2.5f -> "High (${String.format("%.1f", adminState.motionSensitivity)} m/s²)"
                            adminState.motionSensitivity <= 4.5f -> "Medium (${String.format("%.1f", adminState.motionSensitivity)} m/s²)"
                            else -> "Low (${String.format("%.1f", adminState.motionSensitivity)} m/s²)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = adminState.motionSensitivity,
                    onValueChange = onUpdateSensitivity,
                    valueRange = 1.5f..7.0f,
                    modifier = Modifier.testTag("motion_sensitivity_slider")
                )
            }

            // Quick Simulate Shake Button
            OutlinedButton(
                onClick = onSimulateMotion,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_motion_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("⚡ Simulate Phone Movement / Shake Alarm", fontWeight = FontWeight.Bold)
            }
        }
    }
}
