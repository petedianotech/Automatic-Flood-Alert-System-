package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.EmergencyContact
import com.example.data.EvacuationShelter
import com.example.data.LedState
import com.example.ui.theme.HighContrastTextDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusRedText
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg
import com.example.ui.theme.StatusYellowText

@Composable
fun VillagerPortalScreen(
    viewModel: WaterMonitorViewModel,
    villagerState: VillagerUiState,
    latestLog: DetectionLog?,
    contacts: List<EmergencyContact>,
    shelters: List<EvacuationShelter>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLed = try {
        LedState.valueOf(latestLog?.ledState ?: "GREEN")
    } catch (e: Exception) {
        LedState.GREEN
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("🚨 Action Plan", "🏠 Shelters", "📞 Hotlines")

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Minimal Header (Replaces clunky banner to declutter UI)
        item {
            MinimalistHeader()
        }

        // 2. Beautiful Safety Status Card
        item {
            HighContrastSafetyDashboardCard(
                ledState = currentLed,
                latestLog = latestLog,
                isAlarmActive = villagerState.isAlarmActive,
                onToggleAlarm = { viewModel.toggleAlarmSiren() }
            )
        }

        // 3. Tab Navigation Row for secondary content (Drastically reduces visual bloat!)
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        // 4. Tab Content Rendering based on Selection
        when (selectedTabIndex) {
            0 -> {
                // Action Plan Page: Evacuation Instructions + Interactive Go-Bag Checklist
                item {
                    EvacuationInstructionsCard(ledState = currentLed)
                }
                item {
                    EmergencyChecklistCard(
                        checklist = villagerState.checklistItems,
                        onToggleItem = { viewModel.toggleChecklistItem(it) }
                    )
                }
            }
            1 -> {
                // Nearby Shelters Page: Filtered list for high-efficiency response
                if (shelters.isEmpty()) {
                    item {
                        EmptyStateCard(message = "No evacuation shelters mapped near your area.")
                    }
                } else {
                    items(shelters) { shelter ->
                        ShelterItemCard(shelter = shelter, onNavigateClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(shelter.name + ", " + shelter.address)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            }
                        })
                    }
                }
            }
            2 -> {
                // Emergency Rescue & Hotlines Page
                if (contacts.isEmpty()) {
                    item {
                        EmptyStateCard(message = "No rescue contacts listed. In extreme emergencies call local state responders.")
                    }
                } else {
                    items(contacts) { contact ->
                        EmergencyContactCard(contact = contact, onCallClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                            context.startActivity(intent)
                        })
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MinimalistHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "COMMUNITY HUB",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Live Villager Portal",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Real-time safety directives, shelter routing, and local hotlines.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HighContrastSafetyDashboardCard(
    ledState: LedState,
    latestLog: DetectionLog?,
    isAlarmActive: Boolean,
    onToggleAlarm: () -> Unit
) {
    val (containerBg, textColor, statusTitle, statusBody, icon) = when (ledState) {
        LedState.GREEN -> Tuple5(
            StatusGreenBg,
            StatusGreenText,
            "RIVER STATUS: SAFE / GREEN",
            "Water levels are normal. No immediate flood threat detected in your village area.",
            Icons.Default.CheckCircle
        )
        LedState.YELLOW -> Tuple5(
            StatusYellowBg,
            StatusYellowText,
            "RIVER STATUS: WARNING / RISING",
            "Water levels are rising rapidly! Prepare emergency bags and stay tuned for updates.",
            Icons.Default.Warning
        )
        LedState.RED -> Tuple5(
            StatusRedBg,
            StatusRedText,
            "RIVER STATUS: CRITICAL / EVACUATE NOW!",
            "DANGER: Severe flood level reached! EVACUATE TO HIGHER GROUND IMMEDIATELY!",
            Icons.Default.NotificationsActive
        )
        LedState.UNKNOWN -> Tuple5(
            Color(0xFFF1F5F9),
            HighContrastTextDark,
            "STATUS: SENSOR OFFLINE",
            "Sensor reading unavailable. Maintain caution near river banks.",
            Icons.Default.Shield
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alarmScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmPulsing"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("villager_safety_dashboard_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier
                            .size(36.dp)
                            .scale(if (ledState == LedState.RED) scale else 1f)
                    )
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }

            Text(
                text = statusBody,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Water Height: ${latestLog?.waterLevelMeters ?: 1.2} meters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Text(
                        text = "Last verified: ${latestLog?.timestamp?.let { formatTimestamp(it) } ?: "Just now"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }

                // Emergency Audio Siren Toggle Button
                Button(
                    onClick = onToggleAlarm,
                    modifier = Modifier.testTag("siren_alarm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlarmActive) StatusRed else textColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Siren Alarm Toggle",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAlarmActive) "SIREN ON" else "SIREN DEMO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
fun EvacuationInstructionsCard(ledState: LedState) {
    val instructions = when (ledState) {
        LedState.RED -> listOf(
            "1. LEAVE LOW-LYING AREAS IMMEDIATELY!",
            "2. Proceed along designated high-ground route to High School Gymnasium.",
            "3. Do NOT attempt to wade or drive across flooded bridges or spillways.",
            "4. Bring your waterproof Emergency Go-Bag with identification.",
            "5. Assist elderly family members and children."
        )
        LedState.YELLOW -> listOf(
            "1. Pack essential medication, birth certificates, and cash.",
            "2. Charge mobile devices and emergency flashlights.",
            "3. Move livestock and valuable property to elevated ground.",
            "4. Monitor local emergency broadcast speaker and this app.",
            "5. Keep children near home and away from riverbanks."
        )
        LedState.GREEN -> listOf(
            "1. River condition is currently safe and stable.",
            "2. Inspect household drainage and clean gutters periodically.",
            "3. Maintain your emergency supply bag fully stocked.",
            "4. Save emergency contacts to your phone contacts.",
            "5. Report any sensor damage or debris blockage to Village Chief."
        )
        LedState.UNKNOWN -> listOf(
            "1. Maintain routine flood readiness precautions.",
            "2. Avoid crossing swollen rivers during heavy rainfall."
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("evacuation_instructions_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Emergency Action Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            instructions.forEach { text ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ShelterItemCard(shelter: EvacuationShelter, onNavigateClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shelter_card_${shelter.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HomeWork,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shelter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = shelter.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Distance: ${shelter.distanceKm} km",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capacity: ${shelter.currentOccupancy}/${shelter.capacity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = onNavigateClick,
                modifier = Modifier.testTag("shelter_navigate_${shelter.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Navigate to shelter",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmergencyContactCard(contact: EmergencyContact, onCallClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (contact.isHotline) StatusRedBg.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (contact.isHotline) StatusRed else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (contact.isHotline) Icons.Default.LocalHospital else Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = contact.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (contact.isHotline) StatusRedText else MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onCallClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (contact.isHotline) StatusRed else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("CALL", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmergencyChecklistCard(
    checklist: Map<String, Boolean>,
    onToggleItem: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("emergency_checklist_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Emergency Go-Bag Readiness Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            checklist.forEach { (item, isChecked) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleItem(item) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleItem(item) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
