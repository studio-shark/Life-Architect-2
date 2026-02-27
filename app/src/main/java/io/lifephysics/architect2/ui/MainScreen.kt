package io.lifephysics.architect2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.lifephysics.architect2.ui.composables.XpPopup
import io.lifephysics.architect2.ui.screens.AnalyticsScreen
import io.lifephysics.architect2.ui.screens.HistoryScreen
import io.lifephysics.architect2.ui.screens.TasksScreen
import io.lifephysics.architect2.ui.screens.TrendingScreen
import io.lifephysics.architect2.ui.screens.UserScreen
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Tasks,
        Screen.History,
        Screen.User,
        Screen.Trending,
        Screen.Analytics
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            },
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
                composable(Screen.User.route) {
                    UserScreen(
                        uiState = uiState,
                        onThemeChange = { viewModel.onThemeChange(it) }
                    )
                }
                composable(Screen.Trending.route) {
                    val trendsState by viewModel.trendsUiState.collectAsStateWithLifecycle()
                    TrendingScreen(trendsUiState = trendsState)
                }
                composable(Screen.Analytics.route) { AnalyticsScreen() }
            }
        }

        if (uiState.xpPopupVisible) {
            XpPopup(
                amount = uiState.xpPopupAmount,
                isCritical = uiState.xpPopupIsCritical,
                onDismiss = { viewModel.onDismissXpPopup() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
