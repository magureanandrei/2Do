package com.example.learning_test.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val content: String,

    @SerialName("is_complete")
    @ColumnInfo(name = "is_complete")
    val isComplete: Boolean,

    @SerialName("sort_order")
    @ColumnInfo(name = "sort_order")
    val sortOrder: Long,

    @SerialName("topic_id")
    @ColumnInfo(name = "topic_id")
    val topicId: Int,

    @ColumnInfo(name = "supabase_id")
    val supabaseId: Int? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)