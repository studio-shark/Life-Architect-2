package io.lifephysics.architect2.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Tasks     : Screen("tasks",     "Tasks",    Icons.Default.Checklist)
    object History   : Screen("history",   "History",  Icons.Default.History)
    object User      : Screen("user",      "Profile",  Icons.Default.Person)
    object Trending  : Screen("trending",  "Trending", Icons.Default.TrendingUp)
    object Analytics : Screen("analytics", "Analytics",Icons.Default.Analytics)
    object Settings  : Screen("settings",  "Settings", Icons.Default.Settings)
}
