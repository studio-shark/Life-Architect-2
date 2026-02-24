package io.lifephysics.architect2.ui.viewmodel

import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity

/**
 * Represents the complete, self-contained state of the main UI.
 * This single object holds all the data needed to render every screen.
 *
 * [isAddTaskSheetVisible] is the MVI flag that controls the Modal Bottom Sheet.
 * The ViewModel is the single source of truth for this state — the UI never
 * manages sheet visibility with local state.
 */
data class MainUiState(
    val user: UserEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val rankTitle: String = "Initializing...",
    val xpToNextLevel: Int = 1,
    val currentLevelProgress: Float = 0f,
    val isAddTaskSheetVisible: Boolean = false
)
