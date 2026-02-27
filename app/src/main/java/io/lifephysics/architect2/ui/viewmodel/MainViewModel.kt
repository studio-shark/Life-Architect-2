package io.lifephysics.architect2.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

// ---------------------------------------------------------------------------
// XP System Constants
// ---------------------------------------------------------------------------

/** Full XP for the first N tasks per day. Research shows 7 is a generous but realistic daily target. */
private const val FULL_XP_THRESHOLD = 7

/** Reduced XP tier for tasks 8–15 per day. */
private const val HALF_XP_THRESHOLD = 15

/** XP multiplier for the "extra" tier (tasks 8–15). */
private const val HALF_XP_MULTIPLIER = 0.50f

/** XP multiplier for the "grind" tier (tasks 16+). */
private const val GRIND_XP_MULTIPLIER = 0.10f

/** XP fraction kept when repeating the same task title within 24 hours. */
private const val REPETITION_PENALTY = 0.25f

/** Streak bonus multiplier applied to the first few tasks of the day when streak >= 2. */
private const val STREAK_BONUS_MULTIPLIER = 1.25f

/** Number of tasks per day that receive the streak bonus. */
private const val STREAK_BONUS_TASK_COUNT = 3

/** XP awarded for completing the weekly goal. */
private const val WEEKLY_GOAL_XP = 500

/** XP awarded for achieving a 30-day continuous streak. */
private const val MONTHLY_MILESTONE_XP = 2500

/** Minimum streak length to trigger the monthly milestone. */
private const val MONTHLY_MILESTONE_STREAK = 30

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class MainViewModel(
    val repository: AppRepository,
    private val trendsRepository: TrendsRepository,
    appContext: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("life_architect_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _trendsUiState = MutableStateFlow(TrendsUiState())
    val trendsUiState: StateFlow<TrendsUiState> = _trendsUiState

    /** Tracks timestamps of recent completions for velocity damping. */
    private val recentCompletionTimestamps = ArrayDeque<Long>(10)

    init {
        viewModelScope.launch {
            combine(
                repository.getUser(),
                repository.observePendingTasksForUser("local_user"),
                repository.getCompletedTasks()
            ) { user, pending, completed ->
                val level = user?.level ?: 1
                val xp = user?.xp ?: 0
                val xpNeededForNextLevel = getXpNeededForLevel(level)
                val xpNeededForCurrentLevel = getXpNeededForLevel(level - 1)
                val xpInCurrentLevel = xp - xpNeededForCurrentLevel
                val totalXpForThisLevel = xpNeededForNextLevel - xpNeededForCurrentLevel

                MainUiState(
                    user = user,
                    pendingTasks = pending,
                    completedTasks = completed,
                    level = level,
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

    // ---------------------------------------------------------------------------
    // Task Actions
    // ---------------------------------------------------------------------------

    fun onTaskCompleted(task: TaskEntity) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        val now = System.currentTimeMillis()
        val todayEpochDay = now / 86_400_000L

        // 1. Reset daily counter if it's a new day
        val resetUser = if (user.todayResetDay < todayEpochDay) {
            user.copy(tasksCompletedToday = 0, todayResetDay = todayEpochDay)
        } else user

        // 2. Update streak
        val userWithStreak = updateStreak(resetUser, todayEpochDay)

        // 3. Base XP from difficulty
        val difficulty = TaskDifficulty.valueOf(task.difficulty)
        val baseXp = difficulty.xpValue

        // 4. Diminishing returns tier
        val completedToday = userWithStreak.tasksCompletedToday
        val tieredXp = when {
            completedToday < FULL_XP_THRESHOLD -> baseXp
            completedToday < HALF_XP_THRESHOLD -> (baseXp * HALF_XP_MULTIPLIER).toInt()
            else                               -> (baseXp * GRIND_XP_MULTIPLIER).toInt()
        }

        // 5. Repetition penalty — check completed tasks in the last 24 hours
        val recentCompleted = repository.getCompletedTasks().first()
        val recentTitles = recentCompleted
            .filter { it.completedAt != null && (now - it.completedAt) < 86_400_000L }
            .map { it.title.trim().lowercase() }
        val isRepeat = task.title.trim().lowercase() in recentTitles
        val afterRepeatXp = if (isRepeat) (tieredXp * REPETITION_PENALTY).toInt() else tieredXp

        // 6. Velocity damping for rapid batch completions
        recentCompletionTimestamps.addLast(now)
        if (recentCompletionTimestamps.size > 5) recentCompletionTimestamps.removeFirst()
        val batchCount = recentCompletionTimestamps.count { (now - it) < 10_000L }
        val velocityMultiplier = when ((batchCount - 1).coerceAtLeast(0)) {
            0    -> 1.00f
            1    -> 0.90f
            2    -> 0.80f
            3    -> 0.70f
            else -> 0.60f
        }
        val afterVelocityXp = (afterRepeatXp * velocityMultiplier).toInt()

        // 7. Streak bonus on first STREAK_BONUS_TASK_COUNT tasks of the day
        val streakBonusEligible = userWithStreak.dailyStreak >= 2 &&
                completedToday < STREAK_BONUS_TASK_COUNT
        val afterStreakXp = if (streakBonusEligible) {
            (afterVelocityXp * STREAK_BONUS_MULTIPLIER).toInt()
        } else afterVelocityXp

        // 8. Critical hit (disabled on repeat tasks)
        val isXpCritical = !isRepeat && Random.nextFloat() < 0.20f
        val finalXp = if (isXpCritical) {
            val multiplier = 1.5f + Random.nextFloat() * 1.5f
            (afterStreakXp * multiplier).toInt()
        } else afterStreakXp

        // 9. Increment counters and save task
        var finalUser = checkLevelUp(
            userWithStreak.copy(
                xp = userWithStreak.xp + finalXp,
                totalXp = userWithStreak.totalXp + finalXp,
                tasksCompletedToday = userWithStreak.tasksCompletedToday + 1,
                weeklyGoalProgress = userWithStreak.weeklyGoalProgress + 1
            )
        )

        // 10. Weekly goal payout
        val weeklyBonus = checkWeeklyGoal(finalUser)
        if (weeklyBonus > 0) {
            finalUser = checkLevelUp(
                finalUser.copy(
                    xp = finalUser.xp + weeklyBonus,
                    totalXp = finalUser.totalXp + weeklyBonus,
                    weeklyGoalClaimed = true
                )
            )
        }

        // 11. Monthly milestone payout
        val monthlyBonus = checkMonthlyMilestone(finalUser)
        if (monthlyBonus > 0) {
            finalUser = checkLevelUp(
                finalUser.copy(
                    xp = finalUser.xp + monthlyBonus,
                    totalXp = finalUser.totalXp + monthlyBonus,
                    monthlyMilestoneClaimed = true
                )
            )
        }

        repository.updateUser(finalUser)
        repository.updateTask(task.copy(isCompleted = true, completedAt = now))

        val totalBonus = weeklyBonus + monthlyBonus
        showXpPopup(
            amount = finalXp + totalBonus,
            isCritical = isXpCritical || totalBonus > 0
        )
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

    fun onAddTask(title: String, difficulty: TaskDifficulty = TaskDifficulty.MEDIUM) =
        viewModelScope.launch {
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

    /** Sets or updates the user's weekly task completion goal. */
    fun onSetWeeklyGoal(target: Int) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        val currentWeek = getCurrentIsoWeek()
        repository.updateUser(
            user.copy(
                weeklyGoalTarget = target,
                weeklyGoalProgress = if (user.weeklyGoalWeek == currentWeek) user.weeklyGoalProgress else 0,
                weeklyGoalWeek = currentWeek,
                weeklyGoalClaimed = if (user.weeklyGoalWeek == currentWeek) user.weeklyGoalClaimed else false
            )
        )
    }

    // ---------------------------------------------------------------------------
    // Trends Actions
    // ---------------------------------------------------------------------------

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
                it.copy(isLoading = false, trends = result, selectedCountry = countryCode, error = null)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Pop-up State Handlers
    // ---------------------------------------------------------------------------

    fun onDismissXpPopup() = _uiState.update { it.copy(xpPopupVisible = false) }

    private fun showXpPopup(amount: Int, isCritical: Boolean) =
        _uiState.update {
            it.copy(xpPopupVisible = true, xpPopupAmount = amount, xpPopupIsCritical = isCritical)
        }

    // ---------------------------------------------------------------------------
    // Private Helpers
    // ---------------------------------------------------------------------------

    /**
     * Updates the daily streak:
     * - Same day: no change.
     * - Next consecutive day: streak increments.
     * - Gap of 2+ days: streak resets to 1 and monthly milestone flag resets.
     */
    private fun updateStreak(user: UserEntity, todayEpochDay: Long): UserEntity {
        val lastDay = user.lastCompletionDay
        return when {
            lastDay == todayEpochDay     -> user
            lastDay == todayEpochDay - 1 -> user.copy(
                dailyStreak = user.dailyStreak + 1,
                lastCompletionDay = todayEpochDay
            )
            else -> user.copy(
                dailyStreak = 1,
                lastCompletionDay = todayEpochDay,
                monthlyMilestoneClaimed = false
            )
        }
    }

    /**
     * Returns [WEEKLY_GOAL_XP] if the user has met their weekly goal this week and
     * has not yet claimed the payout, otherwise 0.
     */
    private fun checkWeeklyGoal(user: UserEntity): Int {
        if (user.weeklyGoalTarget <= 0) return 0
        if (user.weeklyGoalWeek != getCurrentIsoWeek()) return 0
        if (user.weeklyGoalClaimed) return 0
        return if (user.weeklyGoalProgress >= user.weeklyGoalTarget) WEEKLY_GOAL_XP else 0
    }

    /**
     * Returns [MONTHLY_MILESTONE_XP] if the user has a 30-day streak and has not yet
     * claimed the milestone this streak cycle, otherwise 0.
     */
    private fun checkMonthlyMilestone(user: UserEntity): Int {
        if (user.monthlyMilestoneClaimed) return 0
        return if (user.dailyStreak >= MONTHLY_MILESTONE_STREAK) MONTHLY_MILESTONE_XP else 0
    }

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

    private fun getCurrentIsoWeek(): Int {
        val cal = java.util.Calendar.getInstance().apply {
            minimalDaysInFirstWeek = 4
            firstDayOfWeek = java.util.Calendar.MONDAY
        }
        return cal.get(java.util.Calendar.WEEK_OF_YEAR)
    }
}

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class TrendsUiState(
    val trends: List<TrendItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCountry: String? = null
)
