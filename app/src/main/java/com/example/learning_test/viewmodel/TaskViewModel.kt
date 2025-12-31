package com.example.learning_test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning_test.Models.Task
import com.example.learning_test.SupabaseClient
import com.example.learning_test.ui.UiState
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {

    private val client = SupabaseClient.client

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _topics = MutableStateFlow<List<String>>(emptyList())
    val topics = _topics.asStateFlow()

    init {
        refreshTopics()
    }

    // --- REFRESH TOPICS ---
    fun refreshTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topics = client.from("tasks")
                    .select(columns = Columns.list("topic")) {
                        order("topic", order = Order.ASCENDING)
                    }
                    .decodeList<Map<String, String>>()
                    .mapNotNull { it["topic"] }
                    .distinct()

                _topics.value = topics
            } catch (e: Exception) {
                println("Error fetching topics: ${e.message}")
                _topics.value = emptyList()
            }
        }
    }

    // --- READ ---
    fun readTasks(topicName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UiState.Loading
            try {
                val tasks = client.from("tasks")
                    .select(columns = Columns.list("id", "content", "is_complete", "topic")) {
                        filter { eq("topic", topicName) }
                        order("id", order = Order.ASCENDING)
                    }
                    .decodeList<Task>()

                _state.value = UiState.Success(tasks)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = UiState.Error(e.message ?: "Error loading tasks")
            }
        }
    }

    // --- CREATE ---
    fun createTask(content: String, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newTask = Task(content = content, topic = topic)
                client.from("tasks").insert(newTask)
                readTasks(topic)
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
                    { set("is_complete", !task.is_complete) }
                ) {
                    filter { eq("id", task.id!!) }
                }
                readTasks(task.topic)
            } catch (e: Exception) {
                println("Error updating: ${e.message}")
            }
        }
    }

    // --- UPDATE (Edit Content) ---
    fun updateTaskContent(id: Int, newContent: String, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").update(
                    { set("content", newContent) }
                ) {
                    filter { eq("id", id) }
                }
                readTasks(topic)
            } catch (e: Exception) {
                println("Error updating content: ${e.message}")
            }
        }
    }

    // --- DELETE ---
    fun deleteTask(id: Int, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.from("tasks").delete {
                    filter { eq("id", id) }
                }
                readTasks(topic)
            } catch (e: Exception) {
                println("Error deleting: ${e.message}")
            }
        }
    }
}

