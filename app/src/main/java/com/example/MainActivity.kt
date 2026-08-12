package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.IconButton
import com.example.ui.CreateAccountScreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LedState
import com.example.ui.AdminDashboardScreen
import com.example.ui.VillagerPortalScreen
import com.example.ui.WaterMonitorViewModel
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.WorkbeeTheme

sealed class BottomNavTab(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Admin : BottomNavTab("admin", "Admin Dashboard", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
    object Villager : BottomNavTab("villager", "Villager Portal", Icons.Filled.Shield, Icons.Outlined.Shield)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkbeeTheme {
                WorkbeeAppMain()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbeeAppMain(
    viewModel: WaterMonitorViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("workbee_auth_prefs", Context.MODE_PRIVATE) }

    var isLoggedIn by remember { mutableStateOf(sharedPrefs.getBoolean("is_logged_in", false)) }
    var userName by remember { mutableStateOf(sharedPrefs.getString("user_name", "") ?: "") }
    var userVillage by remember { mutableStateOf(sharedPrefs.getString("user_village", "") ?: "") }
    var userRole by remember { mutableStateOf(sharedPrefs.getString("user_role", "VILLAGER") ?: "VILLAGER") }

    var selectedTab by remember { mutableStateOf<BottomNavTab>(if (userRole == "ADMIN") BottomNavTab.Admin else BottomNavTab.Villager) }

    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    val villagerState by viewModel.villagerState.collectAsStateWithLifecycle()
    val latestLog by viewModel.latestLog.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val contacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
    val shelters by viewModel.evacuationShelters.collectAsStateWithLifecycle()

    val currentLed = try {
        LedState.valueOf(latestLog?.ledState ?: "GREEN")
    } catch (e: Exception) {
        LedState.GREEN
    }

    val ledColor = when (currentLed) {
        LedState.GREEN -> StatusGreen
        LedState.YELLOW -> StatusYellow
        LedState.RED -> StatusRed
        LedState.UNKNOWN -> Color.Gray
    }

    // Intercept with the account creation screen if not authenticated
    if (!isLoggedIn) {
        CreateAccountScreen(
            onAccountCreated = { name, village, role ->
                sharedPrefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_name", name)
                    .putString("user_village", village)
                    .putString("user_role", role)
                    .apply()

                userName = name
                userVillage = village
                userRole = role
                isLoggedIn = true
                selectedTab = if (role == "ADMIN") BottomNavTab.Admin else BottomNavTab.Villager
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ledColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automatic Flood Alert System",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = ledColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = currentLed.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ledColor
                        )
                    }

                    // Quick Top-Bar Logout
                    IconButton(
                        onClick = {
                            sharedPrefs.edit().clear().apply()
                            isLoggedIn = false
                            userName = ""
                            userVillage = ""
                            userRole = "VILLAGER"
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("top_bar_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout icon",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Only show navigation bar if logged in as ADMIN
            if (userRole == "ADMIN") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    listOf(BottomNavTab.Admin, BottomNavTab.Villager).forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.route}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Strict role separation
            if (userRole == "ADMIN" && selectedTab == BottomNavTab.Admin) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    adminState = adminState,
                    latestLog = latestLog,
                    logs = logs
                )
            } else {
                VillagerPortalScreen(
                    viewModel = viewModel,
                    villagerState = villagerState,
                    latestLog = latestLog,
                    contacts = contacts,
                    shelters = shelters,
                    logs = logs,
                    userName = userName,
                    userVillage = userVillage,
                    onLogout = {
                        sharedPrefs.edit().clear().apply()
                        isLoggedIn = false
                        userName = ""
                        userVillage = ""
                        userRole = "VILLAGER"
                    }
                )
            }
        }
    }
}
