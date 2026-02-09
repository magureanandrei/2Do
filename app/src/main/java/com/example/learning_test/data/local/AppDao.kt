package com.example.learning_test.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- TOPICS ---
    @Query("SELECT * FROM topics WHERE is_archived = 0 ORDER BY sort_order ASC")
    fun getAllTopics(): Flow<List<TopicEntity>> // Flow updates UI automatically!

    @Query("SELECT * FROM topics WHERE is_archived = 1 ORDER BY sort_order ASC")
    fun getArchivedTopics(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopic(topicId: Int)

    @Query("DELETE FROM topics")
    suspend fun clearTopics() // For full re-sync

    @Query("SELECT * FROM topics WHERE id = :topicId")
    suspend fun getTopicById(topicId: Int): TopicEntity?

    @Query("SELECT * FROM topics WHERE is_synced = 0")
    suspend fun getUnsyncedTopics(): List<TopicEntity>

    // --- HELPERS FOR SYNC ---
    @Query("SELECT * FROM topics WHERE supabase_id = :supabaseId LIMIT 1")
    suspend fun getTopicBySupabaseId(supabaseId: Int): TopicEntity?

    @Query("SELECT * FROM tasks WHERE supabase_id = :supabaseId LIMIT 1")
    suspend fun getTaskBySupabaseId(supabaseId: Int): TaskEntity?

    @Query("SELECT supabase_id FROM topics WHERE id = :localId")
    suspend fun getSupabaseTopicId(localId: Int): Int?

    // --- TASKS ---
    @Query("SELECT * FROM tasks WHERE topic_id = :topicId ORDER BY sort_order ASC")
    fun getTasksForTopic(topicId: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Int)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE is_synced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    // --- HELPERS FOR SORTING ---
    @Query("SELECT MAX(sort_order) FROM tasks WHERE topic_id = :topicId")
    suspend fun getMaxTaskSortOrder(topicId: Int): Double?

    @Query("SELECT MIN(sort_order) FROM topics")
    suspend fun getMinTopicSortOrder(): Double?
}