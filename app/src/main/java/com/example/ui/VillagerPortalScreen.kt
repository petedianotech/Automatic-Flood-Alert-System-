package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DetectionLog
import com.example.data.EmergencyContact
import com.example.data.EvacuationShelter
import com.example.data.LedState
import com.example.data.Quadruple
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg

@Composable
fun VillagerPortalScreen(
    viewModel: WaterMonitorViewModel,
    villagerState: VillagerUiState,
    latestLog: DetectionLog?,
    contacts: List<EmergencyContact>,
    shelters: List<EvacuationShelter>,
    logs: List<DetectionLog> = emptyList(),
    userName: String = "",
    userVillage: String = "",
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentLed = try {
        LedState.valueOf(latestLog?.ledState ?: "GREEN")
    } catch (e: Exception) {
        LedState.GREEN
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile Section
        ProfileCard(
            userName = userName,
            userVillage = userVillage,
            onLogout = onLogout
        )

        // 2. Real-Time Flood Status Card
        FloodStatusCard(
            ledState = currentLed,
            latestLog = latestLog
        )

        // 3. Dynamic Safety Recommendations & Action Plan
        SafetyRecommendationsActionCard(
            ledState = currentLed
        )
    }
}

@Composable
fun ProfileCard(
    userName: String,
    userVillage: String,
    onLogout: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("villager_profile_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userName.ifBlank { "Community Member" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Village: ${userVillage.ifBlank { "Dzenje Village" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .testTag("profile_logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun FloodStatusCard(
    ledState: LedState,
    latestLog: DetectionLog?
) {
    val (statusLabel, statusColor, statusBg, description) = when (ledState) {
        LedState.GREEN -> Quadruple(
            "GREEN • NORMAL STATUS",
            StatusGreen,
            StatusGreenBg,
            "Everything is okay. No flood threat detected by LED sensor."
        )
        LedState.BLUE -> Quadruple(
            "BLUE • WARNING ALERT",
            StatusBlue,
            StatusBlueBg,
            "Flood warning! Water level is rising. Stand by for community notifications."
        )
        LedState.RED -> Quadruple(
            "RED • CRITICAL DANGER",
            StatusRed,
            StatusRedBg,
            "CRITICAL FLOOD THREAT! PEOPLE MUST RUN AND EVACUATE IMMEDIATELY!"
        )
        else -> Quadruple(
            "UNKNOWN",
            Color.Gray,
            Color(0xFFF5F5F5),
            "Flood sensor monitoring station offline."
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flood_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusBg
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📡 Automatic Optical LED Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor,
                    modifier = Modifier.scale(if (ledState == LedState.RED || ledState == LedState.BLUE) alphaScale else 1.0f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = statusColor.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ledState == LedState.GREEN) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Alert status icon",
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (ledState) {
                            LedState.GREEN -> "Status: Green (Safe)"
                            LedState.BLUE -> "Status: Blue (Warning Alert)"
                            LedState.RED -> "Status: Red (CRITICAL DANGER)"
                            LedState.UNKNOWN -> "Status: Unknown"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyRecommendationsActionCard(
    ledState: LedState
) {
    val (recommendationsTitle, recommendationsColor, instructions) = when (ledState) {
        LedState.GREEN -> Triple(
            "🟢 Everything is Okay — Recommended Guidelines",
            StatusGreen,
            listOf(
                Pair(Icons.Default.Check, "Everything is currently safe. No flood risk detected."),
                Pair(Icons.Default.NotificationsNone, "Notifications will only be sent for Blue and Red status."),
                Pair(Icons.Default.Phone, "Keep local emergency contact numbers accessible.")
            )
        )
        LedState.BLUE -> Triple(
            "🔵 WARNING STATUS — Recommended Safety Actions",
            StatusBlue,
            listOf(
                Pair(Icons.Default.NotificationsActive, "Warning notification sent! Flood water is rising."),
                Pair(Icons.Default.ElectricalServices, "Charge mobile devices and prepare emergency lights."),
                Pair(Icons.Default.Backpack, "Pack essential medicines, dry food, and clean drinking water."),
                Pair(Icons.Default.ArrowUpward, "Move animals, farm assets, and valuables to high ground.")
            )
        )
        LedState.RED -> Triple(
            "🔴 CRITICAL DANGER — PEOPLE MUST RUN NOW!",
            StatusRed,
            listOf(
                Pair(Icons.Default.Warning, "PEOPLE MUST RUN! EVACUATE IMMEDIATELY!"),
                Pair(Icons.Default.DirectionsRun, "Move directly to designated high-ground community shelters."),
                Pair(Icons.Default.Dangerous, "Do NOT attempt to walk or drive across flooded roads or rivers."),
                Pair(Icons.Default.FamilyRestroom, "Ensure children, elderly, and vulnerable neighbors evacuate safely.")
            )
        )
        else -> Triple(
            "Safety Action Plan",
            MaterialTheme.colorScheme.primary,
            listOf(
                Pair(Icons.Default.Info, "Monitor community warnings and stay alert.")
            )
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safety_recommendations_action_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = recommendationsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = recommendationsColor
            )

            HorizontalDivider(color = recommendationsColor.copy(alpha = 0.2f))

            instructions.forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(recommendationsColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Instruction bullet",
                            tint = recommendationsColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (ledState == LedState.RED) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
