package com.example.learning_test.Models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: Int? = null,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_archived") val isArchived: Boolean = false
)

