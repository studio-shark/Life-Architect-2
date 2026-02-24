package io.lifephysics.architect2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lifephysics.architect2.data.AppRepository
import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity
import io.lifephysics.architect2.domain.TaskDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed the database with a local user on first launch if none exists
            val existingUser = repository.getUser().first()
            if (existingUser == null) {
                repository.updateUser(UserEntity())
            }

            // Combine the user and tasks flows into a single UI state update
            combine(repository.getUser(), repository.getAllTasks()) { user, tasks ->
                val level = user?.level ?: 1
                val xp = user?.xp ?: 0
                val xpNeededForCurrentLevel = getXpNeededForLevel(level - 1)
                val xpNeededForNextLevel = getXpNeededForLevel(level)
                val xpInCurrentLevel = xp - xpNeededForCurrentLevel
                val totalXpForThisLevel = xpNeededForNextLevel - xpNeededForCurrentLevel

                MainUiState(
                    user = user,
                    tasks = tasks.sortedBy { it.isCompleted },
                    isLoading = false,
                    rankTitle = getRankTitle(level),
                    xpToNextLevel = xpNeededForNextLevel,
                    currentLevelProgress = if (totalXpForThisLevel > 0)
                        (xpInCurrentLevel.toFloat() / totalXpForThisLevel.toFloat()).coerceIn(0f, 1f)
                    else 0f,
                    isAddTaskSheetVisible = _uiState.value.isAddTaskSheetVisible,
                    themePreference = Theme.valueOf(user?.themePreference ?: Theme.SYSTEM.name)
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onTaskCompleted(task: TaskEntity) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        // Parse the difficulty string back to the enum to get the XP value
        val difficulty = TaskDifficulty.valueOf(task.difficulty)
        val xpGained = difficulty.xpValue
        val coinsGained = xpGained // Simple 1:1 mapping

        val updatedUser = checkLevelUp(
            user.copy(
                xp = user.xp + xpGained,
                totalXp = user.totalXp + xpGained,
                coins = user.coins + coinsGained
            )
        )
        repository.updateUser(updatedUser)
        repository.updateTask(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
    }

    fun onTaskReverted(task: TaskEntity) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        val difficulty = TaskDifficulty.valueOf(task.difficulty)
        val xpLost = difficulty.xpValue
        val coinsLost = xpLost

        val updatedUser = user.copy(
            xp = (user.xp - xpLost).coerceAtLeast(0),
            totalXp = (user.totalXp - xpLost).coerceAtLeast(0),
            coins = (user.coins - coinsLost).coerceAtLeast(0)
        )
        repository.updateUser(updatedUser)
        repository.updateTask(task.copy(isCompleted = false, completedAt = null))
    }

    fun onAddTask(title: String, difficulty: TaskDifficulty) = viewModelScope.launch {
        val newTask = TaskEntity(
            title = title,
            difficulty = difficulty.name, // Store enum name as String
            userId = _uiState.value.user?.googleId ?: "local_user"
        )
        repository.insertTask(newTask)
        onDismissAddTaskSheet()
    }

    fun onShowAddTaskSheet() {
        _uiState.update { it.copy(isAddTaskSheetVisible = true) }
    }

    fun onDismissAddTaskSheet() {
        _uiState.update { it.copy(isAddTaskSheetVisible = false) }
    }

    /**
     * Persists the user's chosen theme preference to the database.
     * The UI will recompose automatically via the StateFlow.
     */
    fun onThemeChange(theme: Theme) {
        viewModelScope.launch {
            repository.updateUserTheme(theme)
        }
    }

    /**
     * Checks whether the user has accumulated enough XP to level up,
     * and increments the level until they have not.
     */
    private fun checkLevelUp(user: UserEntity): UserEntity {
        var current = user
        while (current.xp >= getXpNeededForLevel(current.level)) {
            current = current.copy(level = current.level + 1)
        }
        return current
    }

    private fun getXpNeededForLevel(level: Int): Int {
        if (level <= 0) return 0
        return floor(500 * level.toDouble().pow(1.2)).toInt()
    }

    private fun getRankTitle(level: Int): String = when {
        level < 5 -> "Fragment Seeker"
        level < 10 -> "Momentum Builder"
        level < 20 -> "Pattern Mapper"
        else -> "System Architect"
    }
}
