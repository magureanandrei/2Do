package com.example.learning_test.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.learning_test.Models.Task
import com.example.learning_test.viewmodel.TaskViewModel

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    topic: String,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {}
) {
    val uiState by viewModel.state.collectAsState()
    var newTaskText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Load tasks for this topic when the screen is first displayed
    LaunchedEffect(topic) {
        viewModel.readTasks(topic)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).imePadding()) {

        // Back button and Topic Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = topic,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Action buttons row (Delete All on the left)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Delete All Tasks button - small and on the left
            Button(
                onClick = { viewModel.deleteAllTasksInTopic(topic) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Delete All Tasks", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Archive Topic button
            Button(
                onClick = { /* TODO: Add archive logic */ },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Archive Topic", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newTaskText.isNotBlank()) {
                            viewModel.createTask(newTaskText, topic)
                            newTaskText = ""
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newTaskText.isNotBlank()) {
                    viewModel.createTask(newTaskText, topic)
                    newTaskText = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LIST AREA (READ) ---
        when (val currentState = uiState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${currentState.message}", color = Color.Red)
            is UiState.Success -> {
                LazyColumn(state = listState) {
                    items(currentState.tasks, key = { it.id ?: 0 }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.updateTask(task) },
                            onEdit = { newContent -> viewModel.updateTaskContent(task.id!!, newContent, topic) },
                            onDelete = { viewModel.deleteTask(task.id!!, topic) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(task.content) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Checkbox and Text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.is_complete,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.content,
                    modifier = Modifier.padding(top = 12.dp),
                    textDecoration = if (task.is_complete) TextDecoration.LineThrough else null,
                    color = if (task.is_complete) Color.Gray else Color.Unspecified
                )
            }

            // Right side: Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                // Icon for Edit
                IconButton(onClick = {
                    editText = task.content
                    showEditDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
                }

                // Icon for Delete
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
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

