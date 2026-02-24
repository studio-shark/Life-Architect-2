package io.lifephysics.architect2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.lifephysics.architect2.ui.MainScreen
import io.lifephysics.architect2.ui.theme.LifeArchitect2Theme
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
            LifeArchitect2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the ViewModel instance to our MainScreen
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}