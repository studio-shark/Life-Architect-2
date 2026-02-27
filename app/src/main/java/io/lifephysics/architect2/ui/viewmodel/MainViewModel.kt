package io.lifephysics.architect2.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lifephysics.architect2.data.AppRepository
import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.data.TrendsRepository
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity
import io.lifephysics.architect2.domain.TaskDifficulty
import io.lifephysics.architect2.domain.TrendItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

class MainViewModel(
    private val repository: AppRepository,
    private val trendsRepository: TrendsRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val prefs by lazy {
        appContext.getSharedPreferences("life_architect_prefs", Context.MODE_PRIVATE)
    }

    private val _trendsUiState = MutableStateFlow(TrendsUiState())
    val trendsUiState: StateFlow<TrendsUiState> = _trendsUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existingUser = repository.getUser().first()
            if (existingUser == null) {
                repository.updateUser(UserEntity())
            }

            combine(
                repository.getUser(),
                repository.getAllTasks()
            ) { user, tasks ->
                val level = user?.level ?: 1
                val xp = user?.xp ?: 0
                val xpNeededForCurrentLevel = getXpNeededForLevel(level - 1)
                val xpNeededForNextLevel = getXpNeededForLevel(level)
                val xpInCurrentLevel = xp - xpNeededForCurrentLevel
                val totalXpForThisLevel = xpNeededForNextLevel - xpNeededForCurrentLevel

                MainUiState(
                    user = user,
                    pendingTasks = tasks.filter { !it.isCompleted },
                    completedTasks = tasks.filter { it.isCompleted }
                        .sortedByDescending { it.completedAt },
                    isLoading = false,
                    rankTitle = getRankTitle(level),
                    xpToNextLevel = xpNeededForNextLevel,
                    currentLevelProgress = if (totalXpForThisLevel > 0)
                        (xpInCurrentLevel.toFloat() / totalXpForThisLevel.toFloat()).coerceIn(0f, 1f)
                    else 0f,
                    themePreference = Theme.valueOf(user?.themePreference ?: Theme.SYSTEM.name),
                    xpPopupVisible = _uiState.value.xpPopupVisible,
                    xpPopupAmount = _uiState.value.xpPopupAmount,
                    xpPopupIsCritical = _uiState.value.xpPopupIsCritical
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        val savedCountry = prefs.getString("trends_country", null)
        _trendsUiState.update { it.copy(selectedCountry = savedCountry) }
        loadTrends(savedCountry)
    }

    // --- Task Actions ---

    fun onTaskCompleted(task: TaskEntity) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        val difficulty = TaskDifficulty.valueOf(task.difficulty)
        val baseXp = difficulty.xpValue

        val isXpCritical = Random.nextFloat() < 0.20f
        val xpGained = if (isXpCritical) {
            val multiplier = 1.5f + Random.nextFloat() * 1.5f
            (baseXp * multiplier).toInt()
        } else baseXp

        val updatedUser = checkLevelUp(
            user.copy(
                xp = user.xp + xpGained,
                totalXp = user.totalXp + xpGained
            )
        )
        repository.updateUser(updatedUser)
        repository.updateTask(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))

        showXpPopup(amount = xpGained, isCritical = isXpCritical)
        delay(1800)
        onDismissXpPopup()
    }

    fun onTaskReverted(task: TaskEntity) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        val difficulty = TaskDifficulty.valueOf(task.difficulty)
        val xpLost = difficulty.xpValue

        val updatedUser = user.copy(
            xp = (user.xp - xpLost).coerceAtLeast(0),
            totalXp = (user.totalXp - xpLost).coerceAtLeast(0)
        )
        repository.updateUser(updatedUser)
        repository.updateTask(task.copy(isCompleted = false, completedAt = null))

        showXpPopup(amount = -xpLost, isCritical = false)
        delay(1800)
        onDismissXpPopup()
    }

    fun onAddTask(title: String, difficulty: TaskDifficulty = TaskDifficulty.MEDIUM) = viewModelScope.launch {
        val newTask = TaskEntity(
            title = title,
            difficulty = difficulty.name,
            userId = _uiState.value.user?.googleId ?: "local_user"
        )
        repository.insertTask(newTask)
    }

    fun onThemeChange(theme: Theme) = viewModelScope.launch {
        repository.updateUserTheme(theme)
    }

    // --- Trends Actions ---

    fun loadTrends(countryCode: String?) = viewModelScope.launch {
        _trendsUiState.update { it.copy(isLoading = true, error = null) }
        prefs.edit().putString("trends_country", countryCode).apply()

        val result = trendsRepository.getTrends(countryCode)

        if (result.isEmpty()) {
            _trendsUiState.update {
                it.copy(
                    isLoading = false,
                    error = "Could not load trends. Check your connection and try again."
                )
            }
        } else {
            _trendsUiState.update {
                it.copy(
                    isLoading = false,
                    trends = result,
                    selectedCountry = countryCode,
                    error = null
                )
            }
        }
    }

    // --- Pop-up State Handlers ---

    fun onDismissXpPopup() = _uiState.update { it.copy(xpPopupVisible = false) }

    private fun showXpPopup(amount: Int, isCritical: Boolean) =
        _uiState.update {
            it.copy(
                xpPopupVisible = true,
                xpPopupAmount = amount,
                xpPopupIsCritical = isCritical
            )
        }

    // --- Private Helpers ---

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
        level < 5  -> "Fragment Seeker"
        level < 10 -> "Momentum Builder"
        level < 20 -> "Pattern Mapper"
        else       -> "System Architect"
    }
}

data class TrendsUiState(
    val trends: List<TrendItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCountry: String? = null
)
