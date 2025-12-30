package com.example.learning_test.ui

import com.example.learning_test.Models.Task

sealed class UiState {
    object Loading : UiState()
    data class Success(val tasks: List<Task>) : UiState()
    data class Error(val message: String) : UiState()
}
