package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseResource
import com.example.data.model.StudyNote
import com.example.ui.components.BannerAdWidget
import com.example.ui.components.ShimmerCard
import com.example.ui.viewmodel.RayfeerutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VaultScreen(
    viewModel: RayfeerutViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Study Notes & AI, 1: Course Resource Vault
    val sectionTitles = listOf("Notes & AI Assistant", "Course Vault")

    val scope = rememberCoroutineScope()
    val notes by viewModel.studyNotes.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var activeNoteToEdit by remember { mutableStateOf<StudyNote?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    // AI Study Assistant state
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var aiQueryText by remember { mutableStateOf("") }
    var aiQueryMode by remember { mutableStateOf("Explain") }
    var aiResponseText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_screen")
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

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSection) {
                0 -> NotesSection(
                    notes = notes,
                    onSelectNote = { activeNoteToEdit = it },
                    onCreateNote = { showCreateNoteDialog = true },
                    onAskAiClick = {
                        aiQueryText = ""
                        aiResponseText = ""
                        showAiAssistantDialog = true
                    }
                )
                1 -> ResourceVaultSection(viewModel = viewModel)
            }
        }

        // Secondary Banner Ad at bottom
        BannerAdWidget(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            adUnitTitle = "Student Textbooks & Cloud Storage Drive"
        )
    }

    // Active Note Editor Dialog
    activeNoteToEdit?.let { note ->
        NoteEditorDialog(
            note = note,
            onDismiss = { activeNoteToEdit = null },
            onSave = { updatedContent ->
                viewModel.updateStudyNote(note, updatedContent)
                activeNoteToEdit = null
            },
            onDelete = {
                viewModel.deleteStudyNote(note)
                activeNoteToEdit = null
            },
            onAskAiForNote = { concept ->
                aiQueryText = concept
                aiResponseText = ""
                showAiAssistantDialog = true
            }
        )
    }

    if (showCreateNoteDialog) {
        CreateNoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onCreate = { title, course, content ->
                viewModel.addStudyNote(title, course, content)
                showCreateNoteDialog = false
            }
        )
    }

    if (showAiAssistantDialog) {
        AiStudyAssistantModal(
            initialConcept = aiQueryText,
            isAiLoading = isAiLoading,
            responseText = aiResponseText,
            onDismiss = { showAiAssistantDialog = false },
            onSendQuery = { query, mode ->
                aiQueryMode = mode
                scope.launch {
                    val resp = viewModel.askGeminiStudyAssistant(query, mode)
                    aiResponseText = resp
                }
            },
            onAppendToCurrentNote = { textToAppend ->
                activeNoteToEdit?.let { currentNote ->
                    val updated = currentNote.markdownContent + "\n\n### AI Study Insights\n" + textToAppend
                    viewModel.updateStudyNote(currentNote, updated)
                    viewModel.showNotification("Appended AI insights to note!")
                }
                showAiAssistantDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 1. NOTES & AI ASSISTANT SECTION
// -------------------------------------------------------------
@Composable
fun NotesSection(
    notes: List<StudyNote>,
    onSelectNote: (StudyNote) -> Unit,
    onCreateNote: () -> Unit,
    onAskAiClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // AI Study Assistant Hero Banner
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Ask Gemini Study Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Get real-world analogies, high-yield exam summaries, or quiz questions for any note concept.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onAskAiClick,
                        modifier = Modifier.testTag("ask_gemini_button")
                    ) {
                        Text("Ask AI")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Markdown Notes (${notes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(onClick = onCreateNote) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Note")
                }
            }
        }

        items(notes) { note ->
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(note.lastEdited))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectNote(note) }
                    .testTag("note_card_${note.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = note.courseCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = note.markdownContent.replace("#", "").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    note.aiSummary?.let { summary ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. COURSE RESOURCE VAULT SECTION
// -------------------------------------------------------------
@Composable
fun ResourceVaultSection(viewModel: RayfeerutViewModel) {
    val sampleResources = remember {
        listOf(
            CourseResource(1, "CS401 Midterm Past Papers (2024-2025)", "CS401", "Past Exam Paper", "2.4 MB", 142),
            CourseResource(2, "Raft & Paxos Consensus Lecture Slides", "CS401", "Lecture Slides", "5.1 MB", 98),
            CourseResource(3, "CS310 Normalization & B+ Trees Cheat Sheet", "CS310", "Formula Sheet", "850 KB", 215),
            CourseResource(4, "EE320 TCP/IP Packet Sniffing Lab Manual", "EE320", "Lab Manual", "1.8 MB", 77),
            CourseResource(5, "HUM201 Engineering Ethics Discussion Briefs", "HUM201", "Lecture Slides", "1.2 MB", 45)
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filtered = sampleResources.filter { res ->
        when (selectedFilter) {
            "All" -> true
            else -> res.courseCode == selectedFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Intelligent Course Resource Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Access previous exam question banks, lab handouts, and peer lecture slides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "CS401", "CS310", "EE320", "HUM201").forEach { code ->
                    FilterChip(
                        selected = selectedFilter == code,
                        onClick = { selectedFilter = code },
                        label = { Text(code) }
                    )
                }
            }
        }

        items(filtered) { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = res.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${res.category} • ${res.size} • ${res.downloadCount} downloads",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.showNotification("Saved '${res.title}' to device storage")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS & AI MODAL
// -------------------------------------------------------------
@Composable
fun NoteEditorDialog(
    note: StudyNote,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onAskAiForNote: (String) -> Unit
) {
    var content by remember { mutableStateOf(note.markdownContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Course: ${note.courseCode}", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { onAskAiForNote(note.title) }) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Explain Concept", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("CS401") }
    var content by remember { mutableStateOf("# Topic Title\n\n- Key concepts:\n- Important definitions:") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course Code (e.g. CS401) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Markdown Content *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title, course, content) },
                enabled = title.isNotBlank()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiStudyAssistantModal(
    initialConcept: String,
    isAiLoading: Boolean,
    responseText: String,
    onDismiss: () -> Unit,
    onSendQuery: (String, String) -> Unit,
    onAppendToCurrentNote: (String) -> Unit
) {
    var conceptInput by remember { mutableStateOf(initialConcept) }
    var selectedMode by remember { mutableStateOf("Explain") }
    val modes = listOf("Explain", "Analogy", "Summary", "Quiz")
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ai_study_assistant_modal"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Gemini Study Assistant")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    label = { Text("Concept or Topic to Analyze") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. CAP Theorem, B+ Tree split...") }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modes.forEach { m ->
                        FilterChip(
                            selected = selectedMode == m,
                            onClick = { selectedMode = m },
                            label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (conceptInput.isNotBlank()) {
                            onSendQuery(conceptInput, selectedMode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = conceptInput.isNotBlank() && !isAiLoading
                ) {
                    Text(if (isAiLoading) "Gemini is Thinking..." else "Generate Study Insights")
                }

                if (isAiLoading) {
                    ShimmerCard(height = 100.dp)
                } else if (responseText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    text = responseText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (responseText.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Explanation", responseText))
                        }
                    ) {
                        Text("Copy")
                    }
                    Button(onClick = { onAppendToCurrentNote(responseText) }) {
                        Text("Append to Note")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (responseText.isNotBlank()) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    )
}
