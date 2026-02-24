package io.lifephysics.architect2.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A sealed class representing all possible screens in the app.
 *
 * Using a sealed class for navigation provides type safety and allows for exhaustive
 * `when` statements in the NavHost, preventing navigation to non-existent routes.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Checklist)
    object History : Screen("history", "History", Icons.Default.History)
    object User : Screen("user", "Profile", Icons.Default.Person)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object AvatarVault : Screen("avatar_vault", "Vault", Icons.Default.ShoppingBasket)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
