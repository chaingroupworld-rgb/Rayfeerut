package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

@Composable
fun ProfileDialog(
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, institution: String, major: String, semester: String, role: UserRole) -> Unit,
    onWatchAdForTokens: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.fullName ?: "Alex Chen") }
    var institution by remember { mutableStateOf(userProfile?.institution ?: "Tech University") }
    var major by remember { mutableStateOf(userProfile?.major ?: "Computer Science") }
    var semester by remember { mutableStateOf(userProfile?.semester ?: "Semester 5") }
    var isManager by remember { mutableStateOf(userProfile?.role == UserRole.MESS_MANAGER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("profile_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Student Profile & Settings")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Token Balance & Rewarded Ad CTA
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AI Tokens: ${userProfile?.aiTokens ?: 0}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Used for Gemini routines & parsing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onWatchAdForTokens,
                            modifier = Modifier.testTag("watch_ad_for_tokens_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+3 Tokens")
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("University / College") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("Academic Major") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("Current Semester") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hostel / Mess Manager Role:", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isManager,
                        onCheckedChange = { isManager = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        institution,
                        major,
                        semester,
                        if (isManager) UserRole.MESS_MANAGER else UserRole.STUDENT
                    )
                    onDismiss()
                }
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
