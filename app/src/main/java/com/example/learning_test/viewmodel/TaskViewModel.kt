package com.example.learning_test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning_test.data.TaskRepository
import com.example.learning_test.data.local.TaskEntity
import com.example.learning_test.data.local.TopicEntity
import com.example.learning_test.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // These flows come directly from Room. They update automatically!
    val topics = repository.topics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedTopics = repository.archivedTopics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true // Start loading
            repository.pullFromSupabase()
            _isSyncing.value = false // Stop loading
        }
    }

    fun readTasks(topicId: Int) {
        // We observe the DB flow for this topic
        viewModelScope.launch {
            repository.getTasksForTopic(topicId).collect { tasks ->
                _state.value = UiState.Success(tasks)
            }
        }
    }

    fun createTopic(name: String, onSuccess: (TopicEntity) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTopic = repository.createTopic(name) // Get the object
            launch(Dispatchers.Main) {
                onSuccess(newTopic) // Pass it to UI
            }
        }
    }

    fun createTask(content: String, topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createTask(content, topicId)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaskStatus(task)
        }
    }

    fun updateTaskContent(taskId: Int, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = (_state.value as? UiState.Success)?.tasks ?: return@launch
            val task = currentTasks.find { it.id == taskId } ?: return@launch
            repository.updateTaskContent(task, newContent)
        }
    }

    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        val currentTasks = (_state.value as? UiState.Success)?.tasks ?: return
        if (fromIndex == toIndex) return

        // Calculate new decimal rank
        val prevRank = if (toIndex > 0) currentTasks[toIndex - 1].sortOrder else 0.0
        val nextRank = if (toIndex < currentTasks.size - 1) currentTasks[toIndex + 1].sortOrder else prevRank + 200.0

        // If moving to very top
        val newRank = if (toIndex == 0) currentTasks[0].sortOrder / 2 else (prevRank + nextRank) / 2

        val taskToMove = currentTasks[fromIndex]

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaskOrder(taskToMove, newRank)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task.id)
        }
    }

    fun reorderTopics(fromIndex: Int, toIndex: Int) {
        val currentTopics = topics.value
        if (fromIndex == toIndex || currentTopics.isEmpty()) return

        val prevRank = if (toIndex > 0) currentTopics[toIndex - 1].sortOrder else 0.0
        val nextRank = if (toIndex < currentTopics.size - 1) currentTopics[toIndex + 1].sortOrder else prevRank + 200.0
        val newRank = if (toIndex == 0) currentTopics[0].sortOrder / 2 else (prevRank + nextRank) / 2

        val topicToMove = currentTopics[fromIndex]

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTopicOrder(topicToMove, newRank)
        }
    }

    fun archiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.archiveTopic(topicId)
        }
    }

    fun unarchiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unarchiveTopic(topicId)
        }
    }

    fun deleteTopic(topicId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTopic(topicId)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun updateTopicName(topicId: Int, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTopicName(topicId, newName)
        }
    }
}