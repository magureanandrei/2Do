package com.example.learning_test.ui

import com.example.learning_test.data.local.TaskEntity

sealed class UiState {
    object Loading : UiState()
    data class Success(val tasks: List<TaskEntity>) : UiState()
    data class Error(val message: String) : UiState()
}
