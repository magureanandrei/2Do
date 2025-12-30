package com.example.learning_test.Models

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int? = null, // Nullable because Supabase generates ID on creation
    val content: String,
    val is_complete: Boolean = false,
    val topic: String = "General"
)