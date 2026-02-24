package io.lifephysics.architect2.ui.viewmodel

import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity

/**
 * Represents the complete, self-contained state of the main UI.
 *
 * [pendingTasks] contains tasks that are not yet completed, shown on the Tasks tab.
 * [completedTasks] contains tasks that have been completed, shown on the History tab.
 *
 * [xpPopupAmount] and [xpPopupVisible] drive the XP gain/loss pop-up overlay.
 * [xpPopupIsCritical] is true when the XP gain was a critical hit.
 *
 * [themePreference] drives the theme applied in [MainActivity]. It defaults to
 * [Theme.SYSTEM] so the app follows the device setting on first launch.
 */
data class MainUiState(
    val user: UserEntity? = null,
    val pendingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val rankTitle: String = "Initializing...",
    val xpToNextLevel: Int = 1,
    val currentLevelProgress: Float = 0f,
    val themePreference: Theme = Theme.SYSTEM,
    val xpPopupVisible: Boolean = false,
    val xpPopupAmount: Int = 0,
    val xpPopupIsCritical: Boolean = false
)
