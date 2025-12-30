package com.example.learning_test.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learning_test.viewmodel.TaskViewModel

@Composable
fun TopicSelectionScreen(
    viewModel: TaskViewModel,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topics by viewModel.topics.collectAsState()
    var newTopicText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Refresh topics when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshTopics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Title with Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
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
                style = MaterialTheme.typography.headlineMedium
            )
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
            LazyColumn {
                items(topics) { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTopicSelected(topic) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Topic",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.titleMedium
                            )
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
                            onTopicSelected(newTopicText)
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
}

