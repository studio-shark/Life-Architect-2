package io.lifephysics.architect2.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Tasks      : Screen("tasks",       "Tasks",    Icons.Default.Checklist)
    object History    : Screen("history",     "History",  Icons.Default.History)
    object User       : Screen("user",        "Profile",  Icons.Default.Person)
    object Shop       : Screen("shop",        "Shop",     Icons.Default.Store)
    object Analytics  : Screen("analytics",   "Analytics",Icons.Default.Analytics)
    object AvatarVault: Screen("avatar_vault","Vault",    Icons.Default.ShoppingBasket)
    object Settings   : Screen("settings",    "Settings", Icons.Default.Settings)
}
