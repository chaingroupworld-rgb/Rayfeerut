package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicTask
import com.example.data.model.ClassSchedule
import com.example.data.model.TaskPriority
import com.example.ui.components.ShimmerCard
import com.example.ui.viewmodel.RayfeerutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AcademicsScreen(
    viewModel: RayfeerutViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Tasks, 1: Schedule & Routine, 2: CGPA Predictor
    val sectionTitles = listOf("Tasks & Deadlines", "Class Routine", "CGPA Predictor")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("academics_screen")
    ) {
        // Tab selector
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
            0 -> TasksSection(viewModel = viewModel)
            1 -> RoutineSection(viewModel = viewModel)
            2 -> CgpaPredictorSection(viewModel = viewModel)
        }
    }
}

// -------------------------------------------------------------
// 1. TASKS & NATURAL LANGUAGE AI PARSER
// -------------------------------------------------------------
@Composable
fun TasksSection(viewModel: RayfeerutViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var aiInputText by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Pending") }

    val filteredTasks = tasks.filter { task ->
        when (selectedFilter) {
            "Pending" -> !task.isCompleted
            "Completed" -> task.isCompleted
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // AI Natural Language Intake Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI Task Intake (Gemini Powered)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Type naturally, e.g. \"Submit Physics lab report next Tuesday at 5 PM\" or \"Urgent CS310 project due tomorrow\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = aiInputText,
                        onValueChange = { aiInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("natural_language_input"),
                        placeholder = { Text("Enter prompt to auto-parse deadline...") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (aiInputText.isNotBlank()) {
                                        viewModel.addNaturalLanguageTaskWithAi(aiInputText)
                                        aiInputText = ""
                                    }
                                },
                                enabled = aiInputText.isNotBlank() && !isAiLoading,
                                modifier = Modifier.testTag("submit_ai_task_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send to AI",
                                    tint = if (aiInputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (aiInputText.isNotBlank()) {
                                viewModel.addNaturalLanguageTaskWithAi(aiInputText)
                                aiInputText = ""
                            }
                        }),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isAiLoading) {
                        ShimmerCard(height = 40.dp)
                    }
                }
            }
        }

        // Filter chips and manual add action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pending", "Completed", "All").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier.testTag("manual_add_task_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual")
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No $selectedFilter Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use the Gemini intake above to log your upcoming deadlines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val isDueSoon = (task.dueDate - System.currentTimeMillis()) in 0..(24 * 3600 * 1000L)
                TaskCardItem(
                    task = task,
                    isDueSoon = isDueSoon,
                    onToggle = { viewModel.toggleTaskCompletion(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }
    }

    if (showManualDialog) {
        ManualAddTaskDialog(
            onDismiss = { showManualDialog = false },
            onAdd = { title, course, hours, priority, notes ->
                viewModel.addTask(title, course, hours, priority, notes)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun TaskCardItem(
    task: AcademicTask,
    isDueSoon: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(task.dueDate))

    val borderModifier = if (isDueSoon) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(14.dp))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    task.courseCode?.let { code ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Priority chip
                    val priorityColor = when (task.priority) {
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                        TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
                    }
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            task.notes?.let { n ->
                Text(
                    text = n,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (isDueSoon) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDueSoon) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isDueSoon) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ROUTINE & SMART SYLLABUS IMPORTER
// -------------------------------------------------------------
@Composable
fun RoutineSection(viewModel: RayfeerutViewModel) {
    val schedules by viewModel.schedules.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var showImporterDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Smart Importer CTA
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Routine Importer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Import unstructured syllabus or timetable text directly using Gemini AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showImporterDialog = true },
                        modifier = Modifier.testTag("open_importer_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }

        if (isAiLoading) {
            item {
                ShimmerCard(height = 120.dp)
            }
        }

        // Schedules Grouped By Day
        daysOfWeek.forEach { day ->
            val daySchedules = schedules.filter { it.dayOfWeek.equals(day, ignoreCase = true) }
            if (daySchedules.isNotEmpty()) {
                item {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(daySchedules) { schedule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = schedule.courseCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = schedule.courseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${schedule.timeSlot} • ${schedule.roomNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = schedule.instructor,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImporterDialog) {
        SyllabusImporterDialog(
            onDismiss = { showImporterDialog = false },
            onImport = { text ->
                viewModel.importSyllabusRoutine(text)
                showImporterDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 3. GPA / CGPA PREDICTOR ENGINE
// -------------------------------------------------------------
@Composable
fun CgpaPredictorSection(viewModel: RayfeerutViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()

    var currentCgpaStr by remember { mutableStateOf(userProfile?.currentCgpa?.toString() ?: "3.68") }
    var completedCreditsStr by remember { mutableStateOf("60") }
    var targetCgpaStr by remember { mutableStateOf(userProfile?.targetCgpa?.toString() ?: "3.85") }
    var currentCreditsStr by remember { mutableStateOf("15") }

    val currentCgpa = currentCgpaStr.toDoubleOrNull() ?: 3.68
    val completedCredits = completedCreditsStr.toDoubleOrNull() ?: 60.0
    val targetCgpa = targetCgpaStr.toDoubleOrNull() ?: 3.85
    val currentCredits = currentCreditsStr.toDoubleOrNull() ?: 15.0

    val totalCredits = completedCredits + currentCredits
    // Formula: (Target * Total - Current * Completed) / Current_Credits
    val requiredGpa = if (currentCredits > 0) {
        ((targetCgpa * totalCredits) - (currentCgpa * completedCredits)) / currentCredits
    } else 0.0

    val isAchievable = requiredGpa in 0.0..4.0
    val statusColor = if (isAchievable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "CGPA Target Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Calculate the exact semester GPA required in your upcoming finals to achieve your graduation goal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentCgpaStr,
                            onValueChange = { currentCgpaStr = it },
                            label = { Text("Current CGPA") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = completedCreditsStr,
                            onValueChange = { completedCreditsStr = it },
                            label = { Text("Past Credits") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = targetCgpaStr,
                            onValueChange = { targetCgpaStr = it },
                            label = { Text("Target CGPA") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = currentCreditsStr,
                            onValueChange = { currentCreditsStr = it },
                            label = { Text("Current Credits") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }

        // Output Result Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REQUIRED SEMESTER GPA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f", requiredGpa),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = if (isAchievable) {
                            "🎯 Achievable! Target an average letter grade of ${if (requiredGpa > 3.7) "A / A-" else if (requiredGpa > 3.3) "B+" else "B"} across all $currentCreditsStr credits this term."
                        } else {
                            "⚠️ Target exceeds 4.0 maximum scale. Consider raising your credit load or planning across multiple remaining semesters."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------
@Composable
fun ManualAddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, course: String?, hours: Long, priority: TaskPriority, notes: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("48") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment Deadline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course Code (e.g. CS401)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours until due (e.g. 24, 48, 72)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Priority:", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val h = hours.toLongOrNull() ?: 24L
                        onAdd(title, course.ifBlank { null }, h, priority, notes.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SyllabusImporterDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var text by remember {
        mutableStateOf(
            """
            Algorithms & Complexity - 09:00 AM - 10:30 AM, Lab 3A
            Distributed Computing - 11:00 AM - 12:30 PM, Room 204
            Embedded Microcontrollers - 02:00 PM - 03:30 PM, Hardware Hall
            Software Engineering Ethics - 10:00 AM - 11:30 AM, Seminar 1
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Syllabus Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste text from your university PDF or timetable routine:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onImport(text)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Parse with Gemini")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
