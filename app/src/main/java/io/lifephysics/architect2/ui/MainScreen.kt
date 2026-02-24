package io.lifephysics.architect2.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.lifephysics.architect2.ui.screens.AnalyticsScreen
import io.lifephysics.architect2.ui.screens.AvatarVaultScreen
import io.lifephysics.architect2.ui.screens.AddTaskSheet
import io.lifephysics.architect2.ui.screens.HistoryScreen
import io.lifephysics.architect2.ui.screens.SettingsScreen
import io.lifephysics.architect2.ui.screens.TasksScreen
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

/**
 * The root composable for the entire app.
 * Hosts the Scaffold, bottom navigation bar, NavHost, and the
 * MVI-driven Modal Bottom Sheet for adding new tasks.
 *
 * @param viewModel The single [MainViewModel] instance, provided by MainActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Tasks,
        Screen.History,
        Screen.Analytics,
        Screen.AvatarVault,
        Screen.Settings
    )

    // MVI: Collect the full UI state from the ViewModel as a lifecycle-aware stream
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // The sheet is shown/hidden based on the ViewModel's state, not local UI state.
    // This is the MVI pattern: the ViewModel is the single source of truth.
    if (uiState.isAddTaskSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissAddTaskSheet() },
            sheetState = sheetState
        ) {
            AddTaskSheet { title, difficulty ->
                viewModel.onAddTask(title, difficulty)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddTaskSheet() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tasks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tasks.route) { TasksScreen(viewModel = viewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel = viewModel) }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.AvatarVault.route) { AvatarVaultScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
