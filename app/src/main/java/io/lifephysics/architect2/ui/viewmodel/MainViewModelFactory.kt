package io.lifephysics.architect2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.lifephysics.architect2.data.AppRepository

/**
 * Factory for creating [MainViewModel] with its [AppRepository] dependency.
 * Required because ViewModels with constructor parameters cannot be instantiated
 * by the default ViewModelProvider.
 */
class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
