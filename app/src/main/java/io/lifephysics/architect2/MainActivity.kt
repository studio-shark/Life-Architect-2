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
import io.lifephysics.architect2.data.TrendsRepository
import io.lifephysics.architect2.ui.MainScreen
import io.lifephysics.architect2.ui.composables.XpPopup
import io.lifephysics.architect2.ui.theme.AppTheme
import io.lifephysics.architect2.ui.viewmodel.MainViewModel
import io.lifephysics.architect2.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val repository by lazy { (application as LifeArchitectApplication).repository }
    private val trendsRepository by lazy { TrendsRepository() }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            repository = repository,
            trendsRepository = trendsRepository,
            appContext = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

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
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(viewModel = viewModel)

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
