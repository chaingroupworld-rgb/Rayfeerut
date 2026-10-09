package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.viewmodel.RayfeerutViewModel

@Composable
fun FocusScreen(
    viewModel: RayfeerutViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Pomodoro, 1: Habits & Streaks, 2: Deep Work Analytics
    val sectionTitles = listOf("Pomodoro Timer", "Daily Habits", "Deep Work Stats")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            sectionTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }

        when (selectedSection) {
            0 -> PomodoroSection(viewModel = viewModel)
            1 -> HabitsSection(viewModel = viewModel)
            2 -> DeepWorkAnalyticsSection(viewModel = viewModel)
        }
    }
}

// -------------------------------------------------------------
// 1. POMODORO TIMER SECTION
// -------------------------------------------------------------
@Composable
fun PomodoroSection(viewModel: RayfeerutViewModel) {
    val timerState by viewModel.timerState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    var showTaskPicker by remember { mutableStateOf(false) }

    val progress = remember(timerState.remainingSeconds, timerState.totalSeconds) {
        if (timerState.totalSeconds > 0) {
            1f - (timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat())
        } else 0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "pomodoro_progress")

    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
    ) {
        // Mode Selector: 25m Focus, 5m Short Break, 15m Long Break
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = !timerState.isBreak && timerState.totalSeconds == 25 * 60,
                            onClick = { viewModel.resetTimer(25, false) },
                            label = { Text("Focus (25m)") }
                        )
                        FilterChip(
                            selected = timerState.isBreak && timerState.totalSeconds == 5 * 60,
                            onClick = { viewModel.resetTimer(5, true) },
                            label = { Text("Short Break (5m)") }
                        )
                        FilterChip(
                            selected = timerState.isBreak && timerState.totalSeconds == 15 * 60,
                            onClick = { viewModel.resetTimer(15, true) },
                            label = { Text("Long Break (15m)") }
                        )
                    }
                }
            }
        }

        // Active Subject / Task Tag
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { showTaskPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Task: ${timerState.currentTaskTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Task",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Minimalist Circular Progress Indicator
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .testTag("circular_timer_container"),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = if (timerState.isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Animated progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (timerState.isBreak) "Rest & Recharge" else if (timerState.isRunning) "Deep Focus Active" else "Ready to Focus",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (timerState.isRunning) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons: Play/Pause/Reset
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = { viewModel.resetTimer(if (timerState.isBreak) 5 else 25, timerState.isBreak) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Timer")
                }

                Button(
                    onClick = {
                        if (timerState.isRunning) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer(timerState.currentTaskTitle)
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .width(160.dp)
                        .testTag("play_pause_timer_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timerState.isRunning) "Pause" else "Start Focus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text("Select Focus Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.filter { !it.isCompleted }.forEach { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.startTimer(task.title)
                                    showTaskPicker = false
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = task.title,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) { Text("Close") }
            }
        )
    }
}

// -------------------------------------------------------------
// 2. HABITS & 7+ DAY STREAKS
// -------------------------------------------------------------
@Composable
fun HabitsSection(viewModel: RayfeerutViewModel) {
    val habits by viewModel.habits.collectAsState()
    var showAddHabitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Habit Streaks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Maintain streaks for 7+ days to unlock celebration confetti & reward badges!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddHabitDialog = true },
                        modifier = Modifier.testTag("add_habit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }
        }

        items(habits) { habit ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_card_${habit.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleHabit(habit) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (habit.isCompletedToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Habit",
                                tint = if (habit.isCompletedToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (habit.isCompletedToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (habit.currentStreak >= 7) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🔥 ${habit.currentStreak} Day Streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (habit.currentStreak >= 7) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!habit.isCompletedToday && habit.streakFreezesAvailable > 0) {
                        OutlinedButton(
                            onClick = { viewModel.useStreakFreeze(habit) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Freeze (${habit.streakFreezesAvailable})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("Add Habit") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name (e.g. Read 10 Pages)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addHabit(name)
                            showAddHabitDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. DEEP WORK ANALYTICS SECTION
// -------------------------------------------------------------
@Composable
fun DeepWorkAnalyticsSection(viewModel: RayfeerutViewModel) {
    val sessions by viewModel.pomodoroSessions.collectAsState()
    val totalMinutes = sessions.sumOf { it.durationMinutes }
    val totalHours = totalMinutes / 60.0

    // Group by course or title
    val grouped = sessions.groupBy { it.courseCode ?: "General" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTAL DEEP WORK LOGGED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", totalHours)} Hours",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Across ${sessions.size} completed Pomodoro focus blocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Hours by Course / Subject",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(grouped.entries.toList()) { (subject, sessList) ->
            val subMins = sessList.sumOf { it.durationMinutes }
            val fraction = if (totalMinutes > 0) subMins.toFloat() / totalMinutes.toFloat() else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${subMins}m (${(fraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
