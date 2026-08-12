package com.example.ui

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        AdminDashboardHeader()

        // Camera Permission Request Banner if not granted
        if (!cameraPermissionState.status.isGranted) {
            CameraPermissionCard(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }

        // Camera Viewfinder & Manual Triggers
        CameraViewfinderSection(
            adminState = adminState,
            isPermissionGranted = cameraPermissionState.status.isGranted,
            onOpenCamera = {
                if (cameraPermissionState.status.isGranted) {
                    viewModel.openCamera()
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            onStopCamera = { viewModel.stopCamera() },
            onToggleDetectionMode = { viewModel.toggleDetectionMode(it) },
            onSetSimulationState = { viewModel.setSimulationLedState(it) },
            onCaptureClick = { bitmap -> viewModel.captureAndAnalyze(customBitmap = bitmap) },
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )

        // Camera Auto-Scan Schedule Trigger Slider
        CameraSchedulerSection(
            adminState = adminState,
            onToggleScheduler = { viewModel.toggleCameraScheduler(it) },
            onIntervalChange = { viewModel.updateSchedulerInterval(it) }
        )

        // Real-Time Optical Detection Result
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin Optical Camera Node",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live OpenCV LED Flood Sensor Station",
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
                        text = "ONLINE",
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
fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camera_permission_card"),
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Camera access is needed for live optical LED detection and OpenCV analysis.",
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
                Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraViewfinderSection(
    adminState: AdminUiState,
    isPermissionGranted: Boolean,
    onOpenCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onToggleDetectionMode: (Boolean) -> Unit,
    onSetSimulationState: (LedState) -> Unit,
    onCaptureClick: (Bitmap?) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted && !adminState.isCameraOpen) {
            onOpenCamera()
        }
    }

    DisposableEffect(adminState.isCameraOpen) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camera_viewfinder_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Camera Status Badge
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
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Optical Camera Viewfinder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (adminState.isCameraOpen) StatusGreenBg else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (adminState.isCameraOpen) StatusGreen else Color.Gray)
                        )
                        Text(
                            text = if (adminState.isCameraOpen) "LIVE CAMERA STREAM" else "CAMERA STOPPED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (adminState.isCameraOpen) StatusGreenText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = adminState.isLiveDetectionMode,
                    onClick = { onToggleDetectionMode(true) },
                    label = { Text("📷 Live Camera Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_camera_mode_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = !adminState.isLiveDetectionMode,
                    onClick = { onToggleDetectionMode(false) },
                    label = { Text("🧪 Test Calibration", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_calibration_mode_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Camera Viewfinder Box with HUD Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(
                        2.dp,
                        if (adminState.isCameraOpen) StatusGreen else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isPermissionGranted) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                } else if (adminState.isCameraOpen) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewViewRef = this

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().apply {
                                            setSurfaceProvider(surfaceProvider)
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (exc: Exception) {
                                        android.util.Log.e("AdminCameraViewfinder", "Use case binding failed", exc)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.matchParentSize()
                    )

                    // Live Stream HUD Overlay
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top HUD Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = alphaAnim))
                                    )
                                    Text(
                                        text = "LIVE FEED",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (adminState.isLiveDetectionMode) "OPENCV LIVE CAMERA" else "TEST CALIBRATION",
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Center Targeting Reticle ROI
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.CenterHorizontally)
                                .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "LED SENSOR ROI",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        }

                        // Bottom HUD Info Bar
                        val currentResult = adminState.latestAnalysisResult
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ledColor = when (currentResult?.detectedState) {
                                        LedState.GREEN -> StatusGreen
                                        LedState.BLUE -> StatusBlue
                                        LedState.RED -> StatusRed
                                        else -> Color.Gray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ledColor)
                                    )
                                    Text(
                                        text = "DETECTION: ${currentResult?.detectedState?.name ?: "GREEN"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "Conf: ${((currentResult?.confidence ?: 0.95f) * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    val processedBitmap = adminState.latestAnalysisResult?.processedBitmap
                    if (processedBitmap != null) {
                        Image(
                            bitmap = processedBitmap.asImageBitmap(),
                            contentDescription = "OpenCV Processed Frame",
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera Stopped • Tap 'Open Camera' to view stream",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (adminState.isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Analyzing Optical Frame Colors...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Test Trigger Buttons (Only shown in Test Calibration Mode)
            if (!adminState.isLiveDetectionMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Test Sensor Trigger (Simulate Specific Alert Colors):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onSetSimulationState(LedState.GREEN) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Green", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onSetSimulationState(LedState.BLUE) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Blue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onSetSimulationState(LedState.RED) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Red", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Camera Controls (Open Camera, Stop Camera)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenCamera,
                    enabled = !adminState.isCameraOpen && !adminState.isAnalyzing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_camera_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Camera", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStopCamera,
                    enabled = adminState.isCameraOpen && !adminState.isAnalyzing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stop_camera_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop Camera", fontWeight = FontWeight.Bold)
                }
            }

            // Instant Capture & Analyze Button
            OutlinedButton(
                onClick = {
                    val bitmap = previewViewRef?.bitmap
                    onCaptureClick(bitmap)
                },
                enabled = !adminState.isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_analyze_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture & Run OpenCV LED Scan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraSchedulerSection(
    adminState: AdminUiState,
    onToggleScheduler: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camera_scheduler_card"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Auto Camera Schedule Trigger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Slide to adjust open/close analysis timer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = adminState.isSchedulerEnabled,
                    onCheckedChange = onToggleScheduler,
                    modifier = Modifier.testTag("scheduler_toggle_switch")
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera Scan Frequency:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Every ${adminState.schedulerIntervalSeconds} sec",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Interval Slider
                Slider(
                    value = adminState.schedulerIntervalSeconds.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
                    enabled = adminState.isSchedulerEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_scheduler_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5s (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("30s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("120s (Power Save)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (adminState.isSchedulerEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (adminState.isSchedulerEnabled) Icons.Default.Autorenew else Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = adminState.cameraStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
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
                text = "Last OpenCV Optical Analysis Result",
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
                            text = "LED DETECTED: ${ledState.name}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ledColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (ledState) {
                            LedState.GREEN -> "GREEN - Normal (Everything OK)"
                            LedState.BLUE -> "BLUE - Warning Alert"
                            LedState.RED -> "RED - DANGER EVACUATE NOW!"
                            LedState.UNKNOWN -> "SENSOR UNKNOWN"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ledColor
                    )

                    Text(
                        text = latestLog?.statusSummary ?: "Normal LED status detected.",
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
                "Routine camera monitoring active across flood detection nodes.",
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
            "SENSOR OFFLINE RECOMMENDATIONS",
            Color.Gray,
            Color(0xFFF1F5F9),
            listOf(
                "Camera feed obstructed or re-calibrating.",
                "Check physical camera lens and optical LED sensor housing."
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
