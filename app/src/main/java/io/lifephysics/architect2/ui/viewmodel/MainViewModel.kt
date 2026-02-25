package io.lifephysics.architect2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lifephysics.architect2.data.AppRepository
import io.lifephysics.architect2.data.Theme
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity
import io.lifephysics.architect2.domain.Avatar
import io.lifephysics.architect2.domain.TaskDifficulty
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

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existingUser = repository.getUser().first()
            if (existingUser == null) {
                repository.updateUser(UserEntity())
                // Grant the free starter avatar on first launch
                repository.purchaseAvatar(avatarId = 1, price = 0)
            }

            combine(
                repository.getUser(),
                repository.getAllTasks(),
                repository.getOwnedAvatarIds()
            ) { user, tasks, ownedIds ->
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
                    // Preserve pop-up states across recompositions
                    xpPopupVisible = _uiState.value.xpPopupVisible,
                    xpPopupAmount = _uiState.value.xpPopupAmount,
                    xpPopupIsCritical = _uiState.value.xpPopupIsCritical,
                    coinPopupVisible = _uiState.value.coinPopupVisible,
                    coinPopupAmount = _uiState.value.coinPopupAmount,
                    coinPopupIsCritical = _uiState.value.coinPopupIsCritical,
                    // Avatar state
                    ownedAvatarIds = if (ownedIds.isEmpty()) listOf(1) else ownedIds,
                    equippedAvatarId = user?.equippedAvatarId ?: 1
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
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

        val isCoinCritical = Random.nextFloat() < 0.15f
        val coinsGained = if (isCoinCritical) {
            val multiplier = 3f + Random.nextFloat() * 2f
            (baseXp * multiplier).toInt()
        } else baseXp

        val updatedUser = checkLevelUp(
            user.copy(
                xp = user.xp + xpGained,
                totalXp = user.totalXp + xpGained,
                coins = user.coins + coinsGained
            )
        )
        repository.updateUser(updatedUser)
        repository.updateTask(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))

        showXpPopup(amount = xpGained, isCritical = isXpCritical)
        delay(1800)
        onDismissXpPopup()
        showCoinPopup(amount = coinsGained, isCritical = isCoinCritical)
        delay(2000)
        onDismissCoinPopup()
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

        showXpPopup(amount = -xpLost, isCritical = false)
        delay(1800)
        onDismissXpPopup()
        showCoinPopup(amount = -coinsLost, isCritical = false)
        delay(2000)
        onDismissCoinPopup()
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

    // --- Avatar Shop Actions ---

    /**
     * Attempts to purchase an avatar.
     * Silently returns if the user cannot afford it or already owns it.
     */
    fun onPurchaseAvatar(avatar: Avatar) = viewModelScope.launch {
        val state = _uiState.value
        val user = state.user ?: return@launch
        if (avatar.id in state.ownedAvatarIds) return@launch
        if (user.coins < avatar.price) return@launch
        repository.purchaseAvatar(avatarId = avatar.id, price = avatar.price)
    }

    /**
     * Equips an owned avatar. Silently returns if the avatar is not owned.
     */
    fun onEquipAvatar(avatar: Avatar) = viewModelScope.launch {
        val state = _uiState.value
        if (avatar.id !in state.ownedAvatarIds) return@launch
        repository.equipAvatar(avatarId = avatar.id)
    }

    // --- Pop-up State Handlers ---

    fun onDismissXpPopup() = _uiState.update { it.copy(xpPopupVisible = false) }
    fun onDismissCoinPopup() = _uiState.update { it.copy(coinPopupVisible = false) }

    private fun showXpPopup(amount: Int, isCritical: Boolean) =
        _uiState.update { it.copy(xpPopupVisible = true, xpPopupAmount = amount, xpPopupIsCritical = isCritical) }

    private fun showCoinPopup(amount: Int, isCritical: Boolean) =
        _uiState.update { it.copy(coinPopupVisible = true, coinPopupAmount = amount, coinPopupIsCritical = isCritical) }

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
        level < 5 -> "Fragment Seeker"
        level < 10 -> "Momentum Builder"
        level < 20 -> "Pattern Mapper"
        else -> "System Architect"
    }
}
