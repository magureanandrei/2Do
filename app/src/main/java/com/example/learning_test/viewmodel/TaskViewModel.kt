package com.example.learning_test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning_test.data.TaskRepository
import com.example.learning_test.data.local.TaskEntity
import com.example.learning_test.data.local.TopicEntity
import com.example.learning_test.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // --- TOPICS STATE ---
    // We changed this from a simple val to a MutableStateFlow so we can update it optimistically
    private val _topics = MutableStateFlow<List<TopicEntity>>(emptyList())
    val topics = _topics.asStateFlow()

    // --- TASKS STATE ---
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val archivedTopics = repository.archivedTopics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- JOBS & FLAGS ---
    private var currentReadJob: Job? = null
    private var reorderJob: Job? = null
    private var topicReorderJob: Job? = null

    // THE FIX: Flags to ignore DB updates while dragging
    private var isDraggingTasks = false
    private var isDraggingTopics = false

    init {
        refresh()
        // Start listening to topics immediately
        observeTopics()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            repository.pullFromSupabase()
            _isSyncing.value = false
        }
    }

    // --- TOPIC OBSERVATION ---
    private fun observeTopics() {
        viewModelScope.launch {
            repository.topics.collect { dbTopics ->
                // BLOCKER: If dragging, ignore the DB.
                // We trust the UI state more than the DB right now.
                if (!isDraggingTopics) {
                    _topics.value = dbTopics
                }
            }
        }
    }

    // --- READ TASKS (With Jitter Blocker) ---
    fun readTasks(topicId: Int) {
        currentReadJob?.cancel()
        _state.value = UiState.Loading
        currentReadJob = viewModelScope.launch {
            repository.getTasksForTopic(topicId).collect { tasks ->
                // BLOCKER: If dragging, ignore the DB.
                if (!isDraggingTasks) {
                    _state.value = UiState.Success(tasks)
                }
            }
        }
    }

    // --- REORDER TASKS (Optimistic + Debounce) ---
    fun reorderTasks(topicId: Int, fromIndex: Int, toIndex: Int) {
        val currentList = (_state.value as? UiState.Success)?.tasks?.toMutableList() ?: return
        if (fromIndex == toIndex) return

        // 1. SET FLAG: "I am dragging, don't listen to Room!"
        isDraggingTasks = true

        // 2. VISUAL UPDATE
        if (fromIndex < currentList.size && toIndex <= currentList.size) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _state.value = UiState.Success(currentList)
        }

        // 3. DEBOUNCED SAVE
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch(Dispatchers.IO) {
            // Wait for silence (User stopped moving)
            delay(500)

            // Calculate Math
            val prevRank = if (toIndex > 0) currentList[toIndex - 1].sortOrder else 0L
            val nextRank = if (toIndex < currentList.size - 1) currentList[toIndex + 1].sortOrder else prevRank + 2000L
            val newRank = (prevRank + nextRank) / 2

            // Save
            val item = currentList[toIndex] // Get the item at the new position
            if (newRank == prevRank || newRank == nextRank) {
                repository.rebalanceTasks(topicId)
            } else {
                repository.updateTaskOrder(item.copy(sortOrder = newRank), newRank)
            }

            // 4. RESET FLAG: "Okay, I'm done. Room can speak now."
            // We wait a tiny bit to ensure the DB write triggers the flow update
            delay(100)
            isDraggingTasks = false
        }
    }

    // --- REORDER TOPICS (Optimistic + Debounce) ---
    fun reorderTopics(fromIndex: Int, toIndex: Int) {
        val currentList = _topics.value.toMutableList()
        if (fromIndex == toIndex || currentList.isEmpty()) return

        // 1. SET FLAG
        isDraggingTopics = true

        // 2. VISUAL UPDATE
        if (fromIndex < currentList.size && toIndex <= currentList.size) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _topics.value = currentList // Update the MutableStateFlow!
        }

        // 3. DEBOUNCED SAVE
        topicReorderJob?.cancel()
        topicReorderJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)

            val prevRank = if (toIndex > 0) currentList[toIndex - 1].sortOrder else 0L
            val nextRank = if (toIndex < currentList.size - 1) currentList[toIndex + 1].sortOrder else prevRank + 2000L
            val newRank = (prevRank + nextRank) / 2

            val item = currentList[toIndex]
            repository.updateTopicOrder(item, newRank) // Ensure Repo has this function!

            // 4. RESET FLAG
            delay(100)
            isDraggingTopics = false
        }
    }

    // --- OTHER FUNCTIONS (Unchanged) ---
    fun createTopic(name: String, onSuccess: (TopicEntity) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTopic = repository.createTopic(name)
            launch(Dispatchers.Main) { onSuccess(newTopic) }
        }
    }

    fun createTask(content: String, topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.createTask(content, topicId) }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateTaskStatus(task) }
    }

    fun updateTaskContent(taskId: Int, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = (_state.value as? UiState.Success)?.tasks ?: return@launch
            val task = currentTasks.find { it.id == taskId } ?: return@launch
            repository.updateTaskContent(task, newContent)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTask(task.id) }
    }

    fun archiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.archiveTopic(topicId) }
    }

    fun unarchiveTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.unarchiveTopic(topicId) }
    }

    fun deleteTopic(topicId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTopic(topicId)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun updateTopicName(topicId: Int, newName: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateTopicName(topicId, newName) }
    }
}