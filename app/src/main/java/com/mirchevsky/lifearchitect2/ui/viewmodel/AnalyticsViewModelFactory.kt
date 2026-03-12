package com.mirchevsky.lifearchitect2.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mirchevsky.lifearchitect2.data.AppRepository

/**
 * Factory for creating [AnalyticsViewModel] with its dependencies.
 *
 * @param repository The [AppRepository] to inject.
 * @param appContext The application [Context] used for calendar permission checks
 *                  and the [DeviceCalendarRepository].
 */
class AnalyticsViewModelFactory(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
