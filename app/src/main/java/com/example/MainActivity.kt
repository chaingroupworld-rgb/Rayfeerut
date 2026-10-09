package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.RewardedAdDialog
import com.example.ui.screens.*
import com.example.ui.theme.RayfeerutTheme
import com.example.ui.viewmodel.ActiveTab
import com.example.ui.viewmodel.RayfeerutViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: RayfeerutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            RayfeerutTheme(darkTheme = isDarkTheme) {
                RayfeerutApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RayfeerutApp(viewModel: RayfeerutViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val showConfetti by viewModel.showConfetti.collectAsState()
    val rewardedAdVisible by viewModel.rewardedAdVisible.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showProfileDialog by remember { mutableStateOf(false) }

    // Display in-app snackbar when notification state changes
    LaunchedEffect(notification) {
        notification?.let { notif ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = notif.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.dismissNotification()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rayfeerut_scaffold"),
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Rayfeerut",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentTab.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // AI Token chip with Rewarded Ad action
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("ai_token_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${userProfile?.aiTokens ?: 0}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { viewModel.setRewardedAdVisible(true) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Watch Ad for Tokens",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Theme Toggle
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile Dialog Trigger
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile?.fullName?.take(1)?.uppercase() ?: "A",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 4.dp
            ) {
                val navItems = listOf(
                    Triple(ActiveTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                    Triple(ActiveTab.ACADEMICS, Icons.AutoMirrored.Filled.MenuBook, "Academics"),
                    Triple(ActiveTab.MESS, Icons.Default.Restaurant, "Mess"),
                    Triple(ActiveTab.FOCUS, Icons.Default.Timer, "Focus"),
                    Triple(ActiveTab.VAULT, Icons.Default.Folder, "Vault")
                )

                navItems.forEach { (tab, icon, label) ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { targetTab ->
                when (targetTab) {
                    ActiveTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    ActiveTab.ACADEMICS -> AcademicsScreen(viewModel = viewModel)
                    ActiveTab.MESS -> MessScreen(viewModel = viewModel)
                    ActiveTab.FOCUS -> FocusScreen(viewModel = viewModel)
                    ActiveTab.VAULT -> VaultScreen(viewModel = viewModel)
                }
            }

            // Confetti Overlay for streak achievements & task completion
            ConfettiOverlay(visible = showConfetti)
        }
    }

    // Rewarded Ad Dialog for AI Tokens
    RewardedAdDialog(
        isOpen = rewardedAdVisible,
        onDismiss = { viewModel.setRewardedAdVisible(false) },
        onRewardEarned = {
            viewModel.rewardAiTokens(amount = 3)
        }
    )

    // User Profile Dialog
    if (showProfileDialog) {
        ProfileDialog(
            userProfile = userProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { name, institution, major, semester, role ->
                viewModel.updateUserProfile(name, institution, major, semester, role)
            },
            onWatchAdForTokens = {
                showProfileDialog = false
                viewModel.setRewardedAdVisible(true)
            }
        )
    }
}
