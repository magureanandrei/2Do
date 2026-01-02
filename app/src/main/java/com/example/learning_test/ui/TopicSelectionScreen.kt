package com.example.learning_test.ui

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TopicSelectionScreen(
    viewModel: TaskViewModel,
    onTopicSelected: (Topic) -> Unit,
    onArchiveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topics by viewModel.topics.collectAsState()
    var newTopicText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var topicToDelete by remember { mutableStateOf<Topic?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Refresh topics when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshTopics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Title with Logo and Archive button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "2Do",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            // Archive button
            IconButton(onClick = onArchiveClicked) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = "View Archived Topics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Button to create new topic
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Topic")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Topic")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of existing topics
        if (topics.isEmpty()) {
            Text(
                text = "No topics yet. Create one to get started!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
                viewModel.reorderTopics(from.index, to.index)
            }

            LazyColumn(state = listState) {
                items(topics, key = { it.id ?: it.name }) { topic ->
                    ReorderableItem(reorderableLazyListState, key = topic.id ?: topic.name) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onTopicSelected(topic) },
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Top row: Drag handle, topic name, action icons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Drag handle
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Drag to reorder",
                                        modifier = Modifier.draggableHandle(),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = topic.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Archive Topic icon (box with arrow)
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context.applicationContext, "Topic archived", Toast.LENGTH_SHORT).show()
                                            topic.id?.let { viewModel.archiveTopic(it) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Archive,
                                            contentDescription = "Archive Topic",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Delete All Tasks icon
                                    IconButton(
                                        onClick = {
                                            topicToDelete = topic
                                            showDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete All Tasks",
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
    }

    // Create New Topic Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Topic") },
            text = {
                OutlinedTextField(
                    value = newTopicText,
                    onValueChange = { newTopicText = it },
                    label = { Text("Topic Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicText.isNotBlank()) {
                            viewModel.createTopic(newTopicText) { createdTopic ->
                                onTopicSelected(createdTopic)
                            }
                            newTopicText = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = {
                    newTopicText = ""
                    showCreateDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
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

