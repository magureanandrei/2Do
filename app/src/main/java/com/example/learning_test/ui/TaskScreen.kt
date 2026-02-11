package com.example.learning_test.ui

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.learning_test.data.local.TaskEntity
import com.example.learning_test.data.local.TopicEntity
import com.example.learning_test.ui.theme.DarkRed
import com.example.learning_test.viewmodel.TaskViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    topic: TopicEntity,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onTopicRenamed: (String) -> Unit = {}
) {
    val uiState by viewModel.state.collectAsState()
    var newTaskText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Track when we need to scroll to bottom after adding a task
    var shouldScrollToBottom by remember { mutableStateOf(false) }

    // Delete confirmation dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Track task count to detect when new task is added
    val taskCount = (uiState as? UiState.Success)?.tasks?.size ?: 0

    // Inline editing state for topic name (Trello-style)
    var isEditingTopicName by remember { mutableStateOf(false) }
    var editedTopicName by remember { mutableStateOf(TextFieldValue(topic.name)) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Helper function to create task and flag scroll to bottom
    fun createTaskAndScrollToBottom() {
        if (newTaskText.isNotBlank()) {
            viewModel.createTask(newTaskText, topic.id)
            shouldScrollToBottom = true
            newTaskText = ""
        }
    }

    // Scroll to bottom when task count changes and flag is set
    LaunchedEffect(taskCount) {
        if (shouldScrollToBottom && taskCount > 0) {
            listState.animateScrollToItem(taskCount - 1)
            shouldScrollToBottom = false
        }
    }

    // Request focus when entering edit mode
    LaunchedEffect(isEditingTopicName) {
        if (isEditingTopicName) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    // Load tasks for this topic when the screen is first displayed
    LaunchedEffect(topic.id) {
        viewModel.readTasks(topic.id)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val systemInsets = WindowInsets.systemBars.asPaddingValues()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = systemInsets.calculateTopPadding())
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {

        // Back button and Topic Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Container with dynamic height for topic name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isEditingTopicName) {
                    // Inline edit mode (Trello-style) with border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BasicTextField(
                            value = editedTopicName,
                            onValueChange = { editedTopicName = it },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val newName = editedTopicName.text.trim()
                                    if (newName.isNotBlank() && newName != topic.name) {
                                        viewModel.updateTopicName(topic.id, newName)
                                        onTopicRenamed(newName)
                                    }
                                    isEditingTopicName = false
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                } else {
                    // Display mode - click to edit (with same padding to prevent shift)
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable {
                                // Enter edit mode with text selected
                                isEditingTopicName = true
                                editedTopicName = TextFieldValue(
                                    text = topic.name,
                                    selection = TextRange(0, topic.name.length)
                                )
                            }
                    )
                }
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Archive Topic button - only show for non-archived topics
            if (!topic.isArchived) {
                Button(
                    onClick = {
                        Toast.makeText(context.applicationContext, "Topic archived", Toast.LENGTH_SHORT).show()
                        viewModel.archiveTopic(topic.id)
                        onBackPressed()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Archive Topic", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Restore Topic button - only show for archived topics
            if (topic.isArchived) {
                Button(
                    onClick = {
                        Toast.makeText(context.applicationContext, "Topic restored", Toast.LENGTH_SHORT).show()
                        viewModel.unarchiveTopic(topic.id)
                        onBackPressed()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore Topic", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Delete Topic button
            Button(
                onClick = { showDeleteConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Topic", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        // --- INPUT ROW (CREATE) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                label = { Text("New Task") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = VisualTransformation.None, // Ensures text is visible even with Password type
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Password, // Forces "No Extract UI" (hides white strip)
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { createTaskAndScrollToBottom() }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { createTaskAndScrollToBottom() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LIST AREA (READ) ---
        when (val currentState = uiState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${currentState.message}", color = Color.Red)
            is UiState.Success -> {
                val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
                    viewModel.reorderTasks(topic.id, from.index, to.index)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = systemInsets.calculateBottomPadding() + 80.dp
                    )
                ) {
                    items(currentState.tasks, key = { it.id }) { task ->
                        ReorderableItem(reorderableLazyListState, key = task.id) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
                            TaskItem(
                                task = task,
                                onToggle = { viewModel.updateTask(task) },
                                onEdit = { newContent ->
                                    viewModel.updateTaskContent(task.id, newContent)
                                },
                                onDelete = {
                                    viewModel.deleteTask(task)
                                },
                                elevation = elevation,
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Topic?") },
            text = {
                Text("Are you sure you want to delete \"${topic.name}\" and all its tasks? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTopic(topic.id) {
                            onBackPressed()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    elevation: Dp = 2.dp,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(task.content) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                modifier = dragModifier.padding(end = 8.dp),
                tint = Color.Gray
            )

            // Left side: Checkbox and Text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isComplete,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.content,
                    textDecoration = if (task.isComplete) TextDecoration.LineThrough else null,
                    color = if (task.isComplete) Color.Gray else Color.Unspecified
                )
            }

            // Right side: Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon for Edit
                IconButton(
                    onClick = {
                        editText = task.content
                        showEditDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                // Icon for Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DarkRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Task") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Task Content") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isNotBlank()) {
                        onEdit(editText)
                        showEditDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
