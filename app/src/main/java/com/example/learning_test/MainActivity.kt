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
                                    navController.navigate("task_screen/$topic")
                                }
                            )
                        }

                        // Task Screen with topic parameter
                        composable(
                            route = "task_screen/{topic}",
                            arguments = listOf(navArgument("topic") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val topic = backStackEntry.arguments?.getString("topic") ?: "General"
                            TaskScreen(
                                viewModel = viewModel,
                                topic = topic,
                                onBackPressed = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}

