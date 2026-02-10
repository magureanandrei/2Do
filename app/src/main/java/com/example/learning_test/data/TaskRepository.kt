package com.example.learning_test.data

import com.example.learning_test.SupabaseClient
import com.example.learning_test.data.local.AppDao
import com.example.learning_test.data.local.TaskEntity
import com.example.learning_test.data.local.TopicEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: AppDao) {
    private val client = SupabaseClient.client

    // --- READ ---
    val topics: Flow<List<TopicEntity>> = dao.getAllTopics()
    val archivedTopics: Flow<List<TopicEntity>> = dao.getArchivedTopics()

    fun getTasksForTopic(topicId: Int): Flow<List<TaskEntity>> = dao.getTasksForTopic(topicId)

    // --- SYNC (The "Duplicate Killer") ---
    suspend fun pullFromSupabase() {
        try {
            // 1. Fetch Remote Data
            val remoteTopics = client.from("topics").select {
                order("sort_order", order = Order.ASCENDING)
            }.decodeList<TopicNetwork>()

            val remoteTasks = client.from("tasks").select {
                order("sort_order", order = Order.ASCENDING)
            }.decodeList<TaskNetwork>()

            // 2. Sync Topics (Update if exists, Insert if new)
            remoteTopics.forEach { dto ->
                val existing = dto.id?.let { dao.getTopicBySupabaseId(it) }

                if (existing != null) {
                    // UPDATE: Keep the Local ID, update the content
                    dao.updateTopic(existing.copy(
                        name = dto.name,
                        sortOrder = dto.sortOrder,
                        isArchived = dto.isArchived,
                        isSynced = true
                    ))
                } else {
                    // INSERT: New item from cloud
                    val newEntity = TopicEntity(
                        name = dto.name,
                        sortOrder = dto.sortOrder,
                        isArchived = dto.isArchived,
                        supabaseId = dto.id,
                        isSynced = true
                    )
                    dao.insertTopic(newEntity)
                }
            }

            // 3. Sync Tasks
            remoteTasks.forEach { dto ->
                val existing = dto.id?.let { dao.getTaskBySupabaseId(it) }

                // Map REMOTE Topic ID to LOCAL Topic ID
                val localTopic = dto.topicId.let { dao.getTopicBySupabaseId(it) }

                if (localTopic != null) {
                    if (existing != null) {
                        dao.updateTask(existing.copy(
                            content = dto.content,
                            isComplete = dto.isComplete,
                            sortOrder = dto.sortOrder,
                            topicId = localTopic.id,
                            isSynced = true
                        ))
                    } else {
                        val newEntity = TaskEntity(
                            content = dto.content,
                            topicId = localTopic.id,
                            isComplete = dto.isComplete,
                            sortOrder = dto.sortOrder,
                            supabaseId = dto.id,
                            isSynced = true
                        )
                        dao.insertTask(newEntity)
                    }
                }
            }
        } catch (e: Exception) {
            println("Sync failed: ${e.message}")
        }
    }

    // --- CREATE TOPIC ---
    suspend fun createTopic(name: String): TopicEntity {
        val minRank = dao.getMinTopicSortOrder() ?: 0L
        val newRank = minRank - 1000

        // 1. Save Local
        val newEntity = TopicEntity(name = name, sortOrder = newRank, isArchived = false, isSynced = false)
        val localId = dao.insertTopic(newEntity).toInt()
        val createdEntity = newEntity.copy(id = localId)

        // 2. Push Remote
        try {
            val networkObj = TopicNetwork(
                id = null,
                name = createdEntity.name,
                sortOrder = createdEntity.sortOrder,
                isArchived = createdEntity.isArchived
            )

            val remoteDto = client.from("topics")
                .insert(networkObj) { select() }
                .decodeSingle<TopicNetwork>()

            dao.updateTopic(createdEntity.copy(supabaseId = remoteDto.id, isSynced = true))
        } catch (e: Exception) { println("Push failed: ${e.message}") }

        return createdEntity
    }

    // --- CREATE TASK ---
    suspend fun createTask(content: String, localTopicId: Int) {
        val maxRank = dao.getMaxTaskSortOrder(localTopicId) ?: 0L
        val newRank = maxRank + 1000

        val newEntity = TaskEntity(content = content, topicId = localTopicId, sortOrder = newRank, isComplete = false, isSynced = false)
        val localId = dao.insertTask(newEntity).toInt()

        try {
            // Find Remote Topic ID
            val remoteTopicId = dao.getSupabaseTopicId(localTopicId)

            if (remoteTopicId != null) {
                val networkObj = TaskNetwork(
                    id = null,
                    content = content,
                    isComplete = false,
                    sortOrder = newRank,
                    topicId = remoteTopicId
                )

                val remoteDto = client.from("tasks")
                    .insert(networkObj) { select() }
                    .decodeSingle<TaskNetwork>()

                dao.updateTask(newEntity.copy(id = localId, supabaseId = remoteDto.id, isSynced = true))
            }
        } catch (e: Exception) { println("Push failed: ${e.message}") }
    }

    // --- UPDATE TASK STATUS (Toggle Checkbox) ---
    suspend fun updateTaskStatus(task: TaskEntity) {
        // 1. Toggle Local
        val updatedTask = task.copy(isComplete = !task.isComplete, isSynced = false)
        dao.updateTask(updatedTask)

        // 2. Push Remote
        val supabaseId = task.supabaseId
        if (supabaseId != null) {
            try {
                client.from("tasks").update({
                    set("is_complete", updatedTask.isComplete)
                }) {
                    filter { eq("id", supabaseId) }
                }
                dao.updateTask(updatedTask.copy(isSynced = true))
            } catch (e: Exception) { println("Update failed: ${e.message}") }
        }
    }

    // --- UPDATE TASK CONTENT (Edit Text) ---
    suspend fun updateTaskContent(task: TaskEntity, newContent: String) {
        val updatedTask = task.copy(content = newContent, isSynced = false)
        dao.updateTask(updatedTask)

        val supabaseId = task.supabaseId
        if (supabaseId != null) {
            try {
                client.from("tasks").update({
                    set("content", newContent)
                }) {
                    filter { eq("id", supabaseId) }
                }
                dao.updateTask(updatedTask.copy(isSynced = true))
            } catch (e: Exception) { println("Update content failed: ${e.message}") }
        }
    }

    // --- REORDER TASK (Drag & Drop) ---
    suspend fun updateTaskOrder(task: TaskEntity, newRank: Long) {
        val updatedTask = task.copy(sortOrder = newRank, isSynced = false)
        dao.updateTask(updatedTask)

        val supabaseId = task.supabaseId
        if (supabaseId != null) {
            try {
                client.from("tasks").update({
                    set("sort_order", newRank)
                }) {
                    filter { eq("id", supabaseId) }
                }
                dao.updateTask(updatedTask.copy(isSynced = true))
            } catch (e: Exception) { println("Reorder failed: ${e.message}") }
        }
    }

    // --- REBALANCE LOGIC ---
    suspend fun rebalanceTasks(topicId: Int) {
        val tasks = dao.getTasksList(topicId) // Must be ordered ASC
        var currentRank = 1000L
        tasks.forEach { task ->
            if (task.sortOrder != currentRank) {
                val updated = task.copy(sortOrder = currentRank, isSynced = false)
                dao.updateTask(updated)
                // Push to Supabase
                task.supabaseId?.let { sId ->
                    try {
                        client.from("tasks").update({ set("sort_order", currentRank) }) {
                            filter { eq("id", sId) }
                        }
                        dao.updateTask(updated.copy(isSynced = true))
                    } catch (e: Exception) { println(e) }
                }
            }
            currentRank += 1000
        }
    }

    // --- RENAME TOPIC ---
    suspend fun renameTopic(topicId: Int, newName: String) {
        val topic = dao.getTopicById(topicId) ?: return
        val updatedTopic = topic.copy(name = newName, isSynced = false)
        dao.updateTopic(updatedTopic)

        val supabaseId = topic.supabaseId
        if (supabaseId != null) {
            try {
                client.from("topics").update({
                    set("name", newName)
                }) {
                    filter { eq("id", supabaseId) }
                }
                dao.updateTopic(updatedTopic.copy(isSynced = true))
            } catch (e: Exception) { println("Rename failed: ${e.message}") }
        }
    }

    // --- UPDATE TOPIC NAME (Alias for renameTopic) ---
    suspend fun updateTopicName(topicId: Int, newName: String) {
        renameTopic(topicId, newName)
    }

    // --- REORDER TOPIC (Drag & Drop) ---
    suspend fun updateTopicOrder(topic: TopicEntity, newRank: Long) {
        val updatedTopic = topic.copy(sortOrder = newRank, isSynced = false)
        dao.updateTopic(updatedTopic)

        val supabaseId = topic.supabaseId
        if (supabaseId != null) {
            try {
                client.from("topics").update({
                    set("sort_order", newRank)
                }) {
                    filter { eq("id", supabaseId) }
                }
                dao.updateTopic(updatedTopic.copy(isSynced = true))
            } catch (e: Exception) { println("Reorder topic failed: ${e.message}") }
        }
    }

    // --- DELETE TOPIC (Zombie Fix) ---
    suspend fun deleteTopic(topicId: Int) {
        // 1. Get Supabase ID BEFORE delete
        val topic = dao.getTopicById(topicId)
        val supabaseId = topic?.supabaseId

        // 2. Delete Local
        dao.deleteTopic(topicId)

        // 3. Delete Remote
        if (supabaseId != null) {
            try {
                client.from("topics").delete {
                    filter { eq("id", supabaseId) }
                }
            } catch (e: Exception) { println("Delete topic failed: ${e.message}") }
        }
    }

    // --- DELETE TASK (Zombie Fix) ---
    suspend fun deleteTask(taskId: Int) {
        // 1. Get Supabase ID BEFORE delete
        val task = dao.getTaskById(taskId)
        val supabaseId = task?.supabaseId

        // 2. Delete Local
        dao.deleteTask(taskId)

        // 3. Delete Remote
        if (supabaseId != null) {
            try {
                client.from("tasks").delete {
                    filter { eq("id", supabaseId) }
                }
            } catch (e: Exception) { println("Delete task failed: ${e.message}") }
        }
    }

    // --- ARCHIVE TOPIC ---
    suspend fun archiveTopic(topicId: Int) {
        val topic = dao.getTopicById(topicId) ?: return
        val updatedTopic = topic.copy(isArchived = true, isSynced = false)
        dao.updateTopic(updatedTopic)

        val supabaseId = topic.supabaseId
        if (supabaseId != null) {
            try {
                client.from("topics").update({ set("is_archived", true) }) { filter { eq("id", supabaseId) } }
                dao.updateTopic(updatedTopic.copy(isSynced = true))
            } catch (e: Exception) { println("Archive failed: ${e.message}") }
        }
    }

    suspend fun unarchiveTopic(topicId: Int) {
        val topic = dao.getTopicById(topicId) ?: return
        val updatedTopic = topic.copy(isArchived = false, isSynced = false)
        dao.updateTopic(updatedTopic)

        val supabaseId = topic.supabaseId
        if (supabaseId != null) {
            try {
                client.from("topics").update({ set("is_archived", false) }) { filter { eq("id", supabaseId) } }
                dao.updateTopic(updatedTopic.copy(isSynced = true))
            } catch (e: Exception) { println("Unarchive failed: ${e.message}") }
        }
    }
}

// --- NETWORK DTOs ---

@Serializable
data class TopicNetwork(
    val id: Int? = null,
    val name: String,
    @SerialName("sort_order") val sortOrder: Long,
    @SerialName("is_archived") val isArchived: Boolean
)

@Serializable
data class TaskNetwork(
    val id: Int? = null,
    val content: String,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("sort_order") val sortOrder: Long,
    @SerialName("topic_id") val topicId: Int
)