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
import kotlinx.coroutines.Job

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // These flows come directly from Room. They update automatically!
    val topics = repository.topics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedTopics = repository.archivedTopics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var currentReadJob: Job? = null

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
        currentReadJob?.cancel()
        _state.value = UiState.Loading // <--- GHOST FIX
        currentReadJob = viewModelScope.launch {
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

    fun reorderTasks(topicId: Int, fromIndex: Int, toIndex: Int) {
        val currentList = (_state.value as? UiState.Success)?.tasks?.toMutableList() ?: return
        if (fromIndex == toIndex) return

        // A. Simulate Move (Crash Fix)
        if (fromIndex < currentList.size) {
             val item = currentList.removeAt(fromIndex)
             if (toIndex <= currentList.size) {
                 currentList.add(toIndex, item)
             } else {
                 return // Out of bounds
             }

             // B. Calculate Neighbors
             val prevRank = if (toIndex > 0) currentList[toIndex - 1].sortOrder else 0L
             val nextRank = if (toIndex < currentList.size - 1) currentList[toIndex + 1].sortOrder else prevRank + 2000L

             // C. Calculate New Rank (Integer Math)
             val newRank = (prevRank + nextRank) / 2

             // D. Collision Check
             if (newRank == prevRank || newRank == nextRank) {
                 // GAP IS GONE! Rebalance everything.
                 viewModelScope.launch(Dispatchers.IO) {
                     repository.rebalanceTasks(topicId)
                     // Usage: The UI flow will update automatically after rebalance
                 }
                 return
             }

             // E. Save Normal Move
             viewModelScope.launch(Dispatchers.IO) {
                 repository.updateTaskOrder(item, newRank)
             }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task.id)
        }
    }

    fun reorderTopics(fromIndex: Int, toIndex: Int) {
        val currentTopics = topics.value.toMutableList()
        if (fromIndex == toIndex || currentTopics.isEmpty()) return

        if (fromIndex < currentTopics.size) {
            val item = currentTopics.removeAt(fromIndex)
            if (toIndex <= currentTopics.size) {
                currentTopics.add(toIndex, item)
            } else {
                return
            }

            val prevRank = if (toIndex > 0) currentTopics[toIndex - 1].sortOrder else 0L
            val nextRank = if (toIndex < currentTopics.size - 1) currentTopics[toIndex + 1].sortOrder else prevRank + 2000L
            val newRank = (prevRank + nextRank) / 2

            viewModelScope.launch(Dispatchers.IO) {
                repository.updateTopicOrder(item, newRank)
            }
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