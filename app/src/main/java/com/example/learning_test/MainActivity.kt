package com.example.learning_test

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learning_test.data.TaskRepository
import com.example.learning_test.data.local.AppDatabase
import com.example.learning_test.data.local.TopicEntity
import com.example.learning_test.ui.ArchivedTopicsScreen
import com.example.learning_test.ui.TaskScreen
import com.example.learning_test.ui.TopicSelectionScreen
import com.example.learning_test.ui.theme.Learning_testTheme
import com.example.learning_test.viewmodel.TaskViewModel
import com.example.learning_test.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Force Edge-to-Edge
        enableEdgeToEdge()

        // 2. SET SYSTEM BARS TO LIGHT GREY
        val lightGrey = android.graphics.Color.parseColor("#E0E0E0")

        window.navigationBarColor = lightGrey
        window.statusBarColor = lightGrey

        // FORCE BACKGROUND: Fixes the "White Strip" moving up behind keyboard
        window.setBackgroundDrawable(ColorDrawable(lightGrey))

        // 3. FIX: Disable Contrast Enforcement (Prevents system overrides)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 4. Ensure icons are visible (Dark icons on Light background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true // true = dark icons for light background
            isAppearanceLightStatusBars = true     // true = dark icons for light background
        }

        // 1. Initialize Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.appDao())
        val viewModelFactory = TaskViewModelFactory(repository)

        setContent {
            Learning_testTheme {
                val viewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                val navController = rememberNavController()

                // Store selected topic for navigation
                var selectedTopic by remember { mutableStateOf<TopicEntity?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "topic_screen",
                        modifier = Modifier
                            // We explicitly avoid applying innerPadding here because the individual screens
                            // ("topic_screen", "task_screen") now manage their own Scaffolds and insets.
                            // If we applied it here, we'd get double padding or the "white bar" issue.
                            // We still use consumeWindowInsets to be correct with the system if we wanted,
                            // but simply removing it allows the children to draw fully edge-to-edge.
                    ) {
                        // Topic Selection Screen
                        composable("topic_screen") {
                            TopicSelectionScreen(
                                viewModel = viewModel,
                                onTopicSelected = { topic ->
                                    selectedTopic = topic
                                    navController.navigate("task_screen/${topic.id}")
                                },
                                onArchiveClicked = {
                                    navController.navigate("archived_topics_screen")
                                }
                            )
                        }

                        // Archived Topics Screen
                        composable("archived_topics_screen") {
                            ArchivedTopicsScreen(
                                viewModel = viewModel,
                                onBackPressed = { navController.navigateUp() },
                                onTopicSelected = { topic ->
                                    selectedTopic = topic
                                    navController.navigate("task_screen/${topic.id}")
                                }
                            )
                        }

                        // Task Screen with topic id parameter
                        composable(
                            route = "task_screen/{topicId}",
                            arguments = listOf(navArgument("topicId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val topicId = backStackEntry.arguments?.getInt("topicId") ?: 0
                            // Use the stored topic or find it from viewModel (check both regular and archived)
                            val topics by viewModel.topics.collectAsState()
                            val archivedTopics by viewModel.archivedTopics.collectAsState()
                            val topic = selectedTopic
                                ?: topics.find { it.id == topicId }
                                ?: archivedTopics.find { it.id == topicId }

                            topic?.let { currentTopic ->
                                TaskScreen(
                                    viewModel = viewModel,
                                    topic = currentTopic,
                                    onBackPressed = { navController.navigateUp() },
                                    onTopicRenamed = { newName ->
                                        // Update the selected topic with new name
                                        selectedTopic = currentTopic.copy(name = newName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
