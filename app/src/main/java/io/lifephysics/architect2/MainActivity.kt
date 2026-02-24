package io.lifephysics.architect2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.ui.MainScreen
import io.lifephysics.architect2.ui.composables.XpPopup
import io.lifephysics.architect2.ui.theme.AppTheme
import io.lifephysics.architect2.ui.viewmodel.MainViewModel
import io.lifephysics.architect2.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    // Get the repository instance from the Application class
    private val repository by lazy { (application as LifeArchitectApplication).repository }

    // Create the ViewModel using our custom factory
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Collect the UI state to read the user's theme preference
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            // Resolve the effective dark/light boolean:
            // SYSTEM  → follow the Android device setting (default on first launch)
            // DARK    → always dark
            // LIGHT   → always light
            val isDarkTheme = when (uiState.themePreference) {
                Theme.DARK   -> true
                Theme.LIGHT  -> false
                Theme.SYSTEM -> systemDark
            }

            AppTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Stack the XP pop-up on top of the entire app using a Box
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(viewModel = viewModel)

                        // XP pop-up overlay — shown at the center of the screen
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
            }
        }
    }
}
