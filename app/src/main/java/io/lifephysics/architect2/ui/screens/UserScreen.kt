package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.ui.viewmodel.MainUiState

/**
 * The User screen.
 *
 * Displays the user's profile, progression stats, and houses the app settings
 * (including the theme switcher). This screen replaces the old Analytics tab position.
 *
 * @param uiState The full UI state from [MainViewModel].
 * @param onThemeChange Callback invoked when the user selects a new theme.
 */
@Composable
fun UserScreen(
    uiState: MainUiState,
    onThemeChange: (Theme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // --- Profile Header ---
        Text(
            text = uiState.user?.name ?: "Adventurer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.rankTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Level ${uiState.user?.level ?: 1}  ·  ${uiState.user?.xp ?: 0} XP  ·  ${uiState.user?.coins ?: 0} Coins",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))

        // --- Settings Section ---
        SettingsScreen(
            currentTheme = uiState.themePreference,
            onThemeChange = onThemeChange
        )
    }
}
