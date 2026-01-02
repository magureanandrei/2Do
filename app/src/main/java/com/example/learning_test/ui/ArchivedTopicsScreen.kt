package com.example.learning_test.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.learning_test.Models.Topic
import com.example.learning_test.ui.theme.DarkRed
import com.example.learning_test.viewmodel.TaskViewModel

@Composable
fun ArchivedTopicsScreen(
    viewModel: TaskViewModel,
    onBackPressed: () -> Unit,
    onTopicSelected: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedTopics by viewModel.archivedTopics.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var topicToDelete by remember { mutableStateOf<Topic?>(null) }
    val context = LocalContext.current

    // Refresh archived topics when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshArchivedTopics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Archive,
                contentDescription = "Archive",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Archived Topics",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // List of archived topics
        if (archivedTopics.isEmpty()) {
            Text(
                text = "No archived topics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                items(archivedTopics, key = { it.id ?: it.name }) { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTopicSelected(topic) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Topic name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Topic",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = topic.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                // Restore button
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context.applicationContext, "Topic restored", Toast.LENGTH_SHORT).show()
                                        topic.id?.let { viewModel.unarchiveTopic(it) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Unarchive,
                                        contentDescription = "Restore Topic",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = {
                                        topicToDelete = topic
                                        showDeleteConfirmDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Topic",
                                        tint = DarkRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && topicToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                topicToDelete = null
            },
            title = { Text("Delete Topic?") },
            text = {
                Text("Are you sure you want to delete \"${topicToDelete?.name}\" and all its tasks? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        topicToDelete?.id?.let { viewModel.deleteTopic(it) }
                        showDeleteConfirmDialog = false
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteConfirmDialog = false
                    topicToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

