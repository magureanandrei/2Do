package com.example.learning_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.learning_test.Models.Topic
import com.example.learning_test.ui.TaskScreen
import com.example.learning_test.ui.TopicSelectionScreen
import com.example.learning_test.ui.theme.Learning_testTheme
import com.example.learning_test.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Learning_testTheme {
                val viewModel = remember { TaskViewModel() }
                val navController = rememberNavController()

                // Store selected topic for navigation
                var selectedTopic by remember { mutableStateOf<Topic?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "topic_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Topic Selection Screen
                        composable("topic_screen") {
                            TopicSelectionScreen(
                                viewModel = viewModel,
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
                            // Use the stored topic or find it from viewModel
                            val topics by viewModel.topics.collectAsState()
                            val topic = selectedTopic ?: topics.find { it.id == topicId }

                            topic?.let {
                                TaskScreen(
                                    viewModel = viewModel,
                                    topic = it,
                                    onBackPressed = { navController.navigateUp() },
                                    onTopicRenamed = { newName ->
                                        // Update the selected topic with new name
                                        selectedTopic = it.copy(name = newName)
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

