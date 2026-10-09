package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calc.UserSettlementReport
import com.example.data.model.AcademicTask
import com.example.data.model.ClassSchedule
import com.example.data.model.Habit
import com.example.data.model.TaskPriority
import com.example.ui.components.ShimmerCard
import com.example.ui.viewmodel.ActiveTab
import com.example.ui.viewmodel.RayfeerutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: RayfeerutViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val welcomeGreeting by viewModel.welcomeGreeting.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val messReport by viewModel.messSummaryReport.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    val pendingTasks = tasks.filter { !it.isCompleted }
    val urgentTasks = pendingTasks.filter { task ->
        val diff = task.dueDate - System.currentTimeMillis()
        diff in 0..(24 * 3600 * 1000L)
    }

    val currentUserReport = messReport.memberReports.firstOrNull { it.isCurrentUser }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Welcome Header & AI Dynamic Greeting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hello, ${userProfile?.fullName ?: "Student"} 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${userProfile?.major ?: "Computer Science"} • ${userProfile?.semester ?: "Semester 5"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${userProfile?.aiTokens ?: 0} Tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Dynamic Gemini Greeting
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = welcomeGreeting,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Quick Metrics 2x2 Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Deadlines
                QuickMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Due Tasks",
                    value = "${pendingTasks.size}",
                    subtext = if (urgentTasks.isNotEmpty()) "${urgentTasks.size} urgent (<24h)" else "On track",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = if (urgentTasks.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.selectTab(ActiveTab.ACADEMICS) }
                )

                // Mess Balance
                val balanceVal = currentUserReport?.netBalance ?: 0.0
                val isSurplus = balanceVal >= 0.0
                QuickMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Mess Balance",
                    value = if (isSurplus) "+$${String.format("%.2f", balanceVal)}" else "-$${String.format("%.2f", -balanceVal)}",
                    subtext = if (isSurplus) "Surplus pool" else "Owed to mess",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (isSurplus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    onClick = { viewModel.selectTab(ActiveTab.MESS) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CGPA Tracker
                QuickMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Current CGPA",
                    value = String.format("%.2f", userProfile?.currentCgpa ?: 3.68),
                    subtext = "Target: ${userProfile?.targetCgpa ?: 3.85}",
                    icon = Icons.Default.Grade,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.selectTab(ActiveTab.ACADEMICS) }
                )

                // Habit Streaks
                val completedToday = habits.count { it.isCompletedToday }
                QuickMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Habits Today",
                    value = "$completedToday / ${habits.size}",
                    subtext = "Streaks active",
                    icon = Icons.Default.CheckCircle,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.selectTab(ActiveTab.FOCUS) }
                )
            }
        }

        // Today's Class Routine
        item {
            SectionHeader(
                title = "Today's Schedule",
                actionText = "Full Routine",
                onAction = { viewModel.selectTab(ActiveTab.ACADEMICS) }
            )
            if (schedules.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No classes scheduled for today. Free day for deep study!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(schedules.take(3)) { schedule ->
                        ClassScheduleCard(schedule = schedule)
                    }
                }
            }
        }

        // Urgent Deadlines Section (<24h with glowing amber border)
        item {
            SectionHeader(
                title = "Upcoming Deadlines",
                actionText = "Manage Tasks",
                onAction = { viewModel.selectTab(ActiveTab.ACADEMICS) }
            )
            if (pendingTasks.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉 All assignments completed! Relax or work on your research.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendingTasks.take(3).forEach { task ->
                        val isDueSoon = (task.dueDate - System.currentTimeMillis()) in 0..(24 * 3600 * 1000L)
                        DeadlineItemCard(
                            task = task,
                            isDueSoon = isDueSoon,
                            onToggle = { viewModel.toggleTaskCompletion(task) }
                        )
                    }
                }
            }
        }

        // Quick Deep Work Launch Widget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(ActiveTab.FOCUS) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Focus Pomodoro Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val mins = timerState.remainingSeconds / 60
                            val secs = timerState.remainingSeconds % 60
                            Text(
                                text = if (timerState.isRunning) "Running: ${String.format("%02d:%02d", mins, secs)}" else "25 min Deep Work Ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (timerState.isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.selectTab(ActiveTab.FOCUS) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (timerState.isRunning) "View" else "Start")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("metric_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ClassScheduleCard(schedule: ClassSchedule) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .testTag("class_card_${schedule.courseCode}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = schedule.courseCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = schedule.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = schedule.courseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = schedule.timeSlot,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Room,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${schedule.roomNumber} • ${schedule.instructor}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DeadlineItemCard(
    task: AcademicTask,
    isDueSoon: Boolean,
    onToggle: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(task.dueDate))

    val borderModifier = if (isDueSoon) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(14.dp))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("toggle_task_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.courseCode?.let { code ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isDueSoon) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DUE SOON",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Due: $formattedDate",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isDueSoon) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
