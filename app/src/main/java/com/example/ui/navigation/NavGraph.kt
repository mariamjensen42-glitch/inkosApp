package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.MainViewModel
import com.example.ui.screens.*

/**
 * Navigation routes for the app
 */
object Routes {
    const val BOOKS_LIST = "books_list"
    const val CREATE_BOOK = "create_book"
    const val NOVEL_WORKSPACE = "novel_workspace"
    const val SHORT_WORKSPACE = "short_workspace"
    const val PLAY_WORKSPACE = "play_workspace"
    const val MODEL_CONFIG = "model_config"
    const val SESSION_MANAGEMENT = "session_management"
    const val AGENT_TOOLS = "agent_tools"
    const val PERFORMANCE_MONITOR = "performance_monitor"
    const val TESTING = "testing"
}

/**
 * Main navigation graph
 */
@Composable
fun InkOSNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.BOOKS_LIST
    ) {
        composable(Routes.BOOKS_LIST) {
            BooksListScreen(
                viewModel = viewModel,
                onNavigateToCreateBook = { navController.navigate(Routes.CREATE_BOOK) },
                onNavigateToModelConfig = { navController.navigate(Routes.MODEL_CONFIG) },
                onNavigateToSessions = { navController.navigate(Routes.SESSION_MANAGEMENT) },
                onNavigateToTools = { navController.navigate(Routes.AGENT_TOOLS) },
                onNavigateToPerformance = { navController.navigate(Routes.PERFORMANCE_MONITOR) },
                onNavigateToTesting = { navController.navigate(Routes.TESTING) },
                onBookSelected = { bookId ->
                    viewModel.selectBook(bookId)
                    // Navigate to appropriate workspace based on book type
                    navController.navigate(Routes.NOVEL_WORKSPACE)
                }
            )
        }

        composable(Routes.CREATE_BOOK) {
            CreateBookScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOVEL_WORKSPACE) {
            NovelWorkspaceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SHORT_WORKSPACE) {
            ShortWorkspaceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PLAY_WORKSPACE) {
            PlayWorkspaceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MODEL_CONFIG) {
            ModelConfigScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SESSION_MANAGEMENT) {
            SessionManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AGENT_TOOLS) {
            AgentToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PERFORMANCE_MONITOR) {
            PerformanceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TESTING) {
            TestingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
