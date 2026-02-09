package com.example.learning_test.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // <--- NEEDED FOR SUPABASE
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    @SerialName("sort_order") // <--- TELLS SUPABASE: "Map 'sort_order' to this"
    @ColumnInfo(name = "sort_order") // <--- TELLS ROOM: "Map 'sort_order' to this"
    val sortOrder: Double,

    @SerialName("is_archived")
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,

    // These fields don't exist in Supabase JSON, so we default them to null/true
    @ColumnInfo(name = "supabase_id")
    val supabaseId: Int? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)