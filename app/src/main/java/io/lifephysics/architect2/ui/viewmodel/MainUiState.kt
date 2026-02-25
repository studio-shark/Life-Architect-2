package io.lifephysics.architect2.ui.viewmodel

import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity

data class MainUiState(
    val user: UserEntity? = null,
    val pendingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val rankTitle: String = "Initializing...",
    val xpToNextLevel: Int = 1,
    val currentLevelProgress: Float = 0f,
    val themePreference: Theme = Theme.SYSTEM,

    // XP Pop-up State
    val xpPopupVisible: Boolean = false,
    val xpPopupAmount: Int = 0,
    val xpPopupIsCritical: Boolean = false,

    // Coin Pop-up State
    val coinPopupVisible: Boolean = false,
    val coinPopupAmount: Int = 0,
    val coinPopupIsCritical: Boolean = false,

    // Avatar Shop State
    val ownedAvatarIds: List<Int> = listOf(1), // Avatar 1 is always owned (free starter)
    val equippedAvatarId: Int = 1
)
