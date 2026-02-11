package com.example.learning_test.ui

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.learning_test.data.local.TopicEntity
import com.example.learning_test.ui.theme.DarkRed
import com.example.learning_test.viewmodel.TaskViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TopicSelectionScreen(
    viewModel: TaskViewModel,
    onTopicSelected: (TopicEntity) -> Unit,
    onArchiveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topics by viewModel.topics.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var newTopicText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Refresh data when screen opens
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

        // --- HEADER (Logo + Sync + Archive) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "2Do",
                style = MaterialTheme.typography.headlineMedium
            )

            // Sync Indicator (Spinner)
            if (isSyncing) {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Archive Button
            IconButton(onClick = onArchiveClicked) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = "Archived",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- CREATE BUTTON ---
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Topic")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Topic")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TOPIC LIST ---
        if (topics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No topics yet. Create one to get started!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
                viewModel.reorderTopics(from.index, to.index)
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    ReorderableItem(reorderableLazyListState, key = topic.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onTopicSelected(topic) }, // Normal click opens topic
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Drag Handle
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Drag to reorder",
                                        modifier = Modifier.draggableHandle(),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Topic Name
                                    Text(
                                        text = topic.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Archive Action
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "Topic archived", Toast.LENGTH_SHORT).show()
                                            viewModel.archiveTopic(topic.id)
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

                                    // Delete Action
                                    IconButton(
                                        onClick = {
                                            topicToDelete = topic
                                            showDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
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
    }

    // --- DIALOGS ---

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTopicText.isNotBlank()) {
                                // Call ViewModel and Navigate when done
                                viewModel.createTopic(newTopicText) { createdTopic ->
                                    showCreateDialog = false
                                    newTopicText = ""
                                    onTopicSelected(createdTopic)
                                }
                            }
                        }
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicText.isNotBlank()) {
                            // Call ViewModel and Navigate when done
                            viewModel.createTopic(newTopicText) { createdTopic ->
                                showCreateDialog = false
                                newTopicText = ""
                                onTopicSelected(createdTopic) // <--- NAVIGATION HAPPENS HERE
                            }
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
                Text("Are you sure you want to delete \"${topicToDelete?.name}\" and all its tasks?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        topicToDelete?.let { viewModel.deleteTopic(it.id) {} }
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