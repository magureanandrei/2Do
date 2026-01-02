package com.example.learning_test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning_test.Models.Task
import com.example.learning_test.Models.Topic
import com.example.learning_test.SupabaseClient
import com.example.learning_test.ui.UiState
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {

    private val client = SupabaseClient.client

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics = _topics.asStateFlow()

    private val _archivedTopics = MutableStateFlow<List<Topic>>(emptyList())
    val archivedTopics = _archivedTopics.asStateFlow()

    init {
        refreshTopics()
    }

    // --- REFRESH TOPICS ---
    fun refreshTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topicsList = client.from("topics")
                    .select {
                        filter { eq("is_archived", false) }
                        order("sort_order", order = Order.ASCENDING)
                    }
                    .decodeList<Topic>()

                _topics.value = topicsList
            } catch (e: Exception) {
                println("Error fetching topics: ${e.message}")
                _topics.value = emptyList()
            }
        }
    }

    // --- REFRESH ARCHIVED TOPICS ---
    fun refreshArchivedTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topicsList = client.from("topics")
                    .select {
                        filter { eq("is_archived", true) }
                        order("sort_order", order = Order.ASCENDING)
                    }
                    .decodeList<Topic>()

                _archivedTopics.value = topicsList
            } catch (e: Exception) {
                println("Error fetching archived topics: ${e.message}")
                _archivedTopics.value = emptyList()
            }
        }
    }

    // --- CREATE TOPIC ---
    fun createTopic(name: String, onSuccess: (Topic) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Shift all existing topics down by incrementing their sort_order
                _topics.value.forEach { topic ->
                    topic.id?.let { id ->
                        client.from("topics").update(
                            { set("sort_order", topic.sortOrder + 1) }
                        ) {
                            filter { eq("id", id) }
                        }
                    }
                }

                // Insert new topic at top with sort_order = 0
                val newTopic = Topic(name = name, sortOrder = 0)

                val insertedTopic = client.from("topics")
                    .insert(newTopic) { select() }
                    .decodeSingle<Topic>()

                refreshTopics()
                launch(Dispatchers.Main) {
                    onSuccess(insertedTopic)
                }
            } catch (e: Exception) {
                println("Error creating topic: ${e.message}")
            }
        }
    }

    // --- READ TASKS ---
    fun readTasks(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UiState.Loading
            try {
                val tasks = client.from("tasks")
                    .select {
                        filter { eq("topic_id", topicId) }
                        order("sort_order", order = Order.ASCENDING)
                    }
                    .decodeList<Task>()

                _state.value = UiState.Success(tasks)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = UiState.Error(e.message ?: "Error loading tasks")
            }
        }
    }

    // --- CREATE TASK ---
    fun createTask(content: String, topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Shift all existing tasks in this topic down by incrementing their sort_order
                val currentTasks = (_state.value as? UiState.Success)?.tasks ?: emptyList()
                currentTasks.forEach { task ->
                    task.id?.let { id ->
                        client.from("tasks").update(
                            { set("sort_order", task.sortOrder + 1) }
                        ) {
                            filter { eq("id", id) }
                        }
                    }
                }

                // Insert new task at top with sort_order = 0
                val newTask = Task(content = content, topicId = topicId, sortOrder = 0)
                client.from("tasks").insert(newTask)
                readTasks(topicId)
            } catch (e: Exception) {
                println("Error creating: ${e.message}")
            }
        }
    }

    // --- UPDATE (Toggle Checkbox) ---
    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").update(
                    { set("is_complete", !task.isComplete) }
                ) {
                    filter { eq("id", task.id!!) }
                }
                readTasks(task.topicId)
            } catch (e: Exception) {
                println("Error updating: ${e.message}")
            }
        }
    }

    // --- UPDATE (Edit Content) ---
    fun updateTaskContent(id: Int, newContent: String, topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").update(
                    { set("content", newContent) }
                ) {
                    filter { eq("id", id) }
                }
                readTasks(topicId)
            } catch (e: Exception) {
                println("Error updating content: ${e.message}")
            }
        }
    }

    // --- DELETE TASK ---
    fun deleteTask(id: Int, topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").delete {
                    filter { eq("id", id) }
                }
                readTasks(topicId)
            } catch (e: Exception) {
                println("Error deleting: ${e.message}")
            }
        }
    }

    // --- DELETE ALL TASKS IN TOPIC ---
    fun deleteAllTasksInTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").delete {
                    filter { eq("topic_id", topicId) }
                }
                readTasks(topicId)
            } catch (e: Exception) {
                println("Error deleting all tasks: ${e.message}")
            }
        }
    }

    // --- DELETE TOPIC (with all its tasks) ---
    fun deleteTopic(topicId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First delete all tasks in the topic
                client.from("tasks").delete {
                    filter { eq("topic_id", topicId) }
                }
                // Then delete the topic itself
                client.from("topics").delete {
                    filter { eq("id", topicId) }
                }
                refreshTopics()
                refreshArchivedTopics()
                launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                println("Error deleting topic: ${e.message}")
            }
        }
    }

    // --- RENAME TOPIC ---
    fun renameTopic(topicId: Int, newName: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("topics").update(
                    { set("name", newName) }
                ) {
                    filter { eq("id", topicId) }
                }
                refreshTopics()
                launch(Dispatchers.Main) {
                    onSuccess(newName)
                }
            } catch (e: Exception) {
                println("Error renaming topic: ${e.message}")
            }
        }
    }

    // --- ARCHIVE TOPIC ---
    fun archiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("topics").update(
                    { set("is_archived", true) }
                ) {
                    filter { eq("id", topicId) }
                }
                refreshTopics()
                refreshArchivedTopics()
            } catch (e: Exception) {
                println("Error archiving topic: ${e.message}")
            }
        }
    }

    // --- UNARCHIVE TOPIC ---
    fun unarchiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("topics").update(
                    { set("is_archived", false) }
                ) {
                    filter { eq("id", topicId) }
                }
                refreshTopics()
                refreshArchivedTopics()
            } catch (e: Exception) {
                println("Error unarchiving topic: ${e.message}")
            }
        }
    }

    // --- REORDER TASKS ---
    fun reorderTasks(topicId: Int, fromIndex: Int, toIndex: Int) {
        val currentTasks = (_state.value as? UiState.Success)?.tasks?.toMutableList() ?: return
        if (fromIndex == toIndex) return

        // Reorder the local list immediately for responsive UI
        val item = currentTasks.removeAt(fromIndex)
        currentTasks.add(toIndex, item)
        _state.value = UiState.Success(currentTasks)

        // Persist new order to database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentTasks.forEachIndexed { index, task ->
                    task.id?.let { id ->
                        client.from("tasks").update(
                            { set("sort_order", index) }
                        ) {
                            filter { eq("id", id) }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error persisting task order: ${e.message}")
            }
        }
    }

    // --- REORDER TOPICS ---
    fun reorderTopics(fromIndex: Int, toIndex: Int) {
        val currentTopics = _topics.value.toMutableList()
        if (fromIndex == toIndex) return

        // Reorder the local list immediately for responsive UI
        val item = currentTopics.removeAt(fromIndex)
        currentTopics.add(toIndex, item)
        _topics.value = currentTopics

        // Persist new order to database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentTopics.forEachIndexed { index, topic ->
                    topic.id?.let { id ->
                        client.from("topics").update(
                            { set("sort_order", index) }
                        ) {
                            filter { eq("id", id) }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error persisting topic order: ${e.message}")
            }
        }
    }
}

