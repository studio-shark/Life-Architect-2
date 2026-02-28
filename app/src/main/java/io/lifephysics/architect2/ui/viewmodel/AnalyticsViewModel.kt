package io.lifephysics.architect2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lifephysics.architect2.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Holds the aggregated analytics data for the Analytics screen.
 *
 * @property totalTasksCompleted Total number of tasks the user has ever completed.
 * @property currentStreak The user's current daily streak count.
 * @property totalXp The user's total accumulated XP across all time.
 * @property dailyCompletions A map of [LocalDate] to task count for the last 14 days.
 * @property streakHeatmapData The set of dates in the last 90 days on which at least one task was completed.
 * @property onTimeCompletions Number of tasks with a due date that were completed on time.
 * @property overdueCompletions Number of tasks with a due date that were completed after the due date.
 * @property bestDay The date and count of the user's most productive single day.
 * @property bestWeek The week-start date and count of the user's most productive week.
 * @property isLoading True while data is being loaded from the database.
 */
data class AnalyticsUiState(
    val totalTasksCompleted: Int = 0,
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val dailyCompletions: Map<LocalDate, Int> = emptyMap(),
    val streakHeatmapData: Set<LocalDate> = emptySet(),
    val onTimeCompletions: Int = 0,
    val overdueCompletions: Int = 0,
    val bestDay: Pair<LocalDate, Int>? = null,
    val bestWeek: Pair<LocalDate, Int>? = null,
    val isLoading: Boolean = true
)

/**
 * ViewModel for the Analytics screen.
 *
 * Combines the user entity and completed task list from [AppRepository] to derive
 * all analytics metrics reactively. All computation is performed off the main thread
 * inside [viewModelScope].
 */
class AnalyticsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                repository.getUser(),
                repository.getCompletedTasks()
            ) { user, completedTasks ->
                if (user == null) return@combine AnalyticsUiState(isLoading = false)

                val zone = ZoneId.systemDefault()
                val now = LocalDate.now()
                val weekFields = WeekFields.of(Locale.getDefault())

                // Map each completed task to the date it was completed
                val tasksByDate = completedTasks
                    .filter { it.completedAt != null }
                    .groupBy { task ->
                        Instant.ofEpochMilli(task.completedAt!!).atZone(zone).toLocalDate()
                    }

                // Daily completions for the last 14 days
                val dailyCompletions = (0..13).associate { offset ->
                    val day = now.minusDays(offset.toLong())
                    day to (tasksByDate[day]?.size ?: 0)
                }

                // Best single day
                val bestDay = tasksByDate
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }

                // Weekly completions and best week
                val weeklyCompletions = completedTasks
                    .filter { it.completedAt != null }
                    .groupBy { task ->
                        val date = Instant.ofEpochMilli(task.completedAt!!).atZone(zone).toLocalDate()
                        date.with(weekFields.dayOfWeek(), 1) // Normalize to week start (Monday)
                    }
                val bestWeek = weeklyCompletions
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }

                // Heatmap: set of active days in the last 90 days
                val heatmapStart = now.minusDays(89)
                val heatmapData = tasksByDate.keys.filter { it >= heatmapStart }.toSet()

                // Due date performance
                val onTime = completedTasks.count { task ->
                    task.dueDate != null && task.completedAt != null && task.completedAt <= task.dueDate
                }
                val overdue = completedTasks.count { task ->
                    task.dueDate != null && task.completedAt != null && task.completedAt > task.dueDate
                }

                AnalyticsUiState(
                    totalTasksCompleted = completedTasks.size,
                    currentStreak = user.dailyStreak,
                    totalXp = user.totalXp,
                    dailyCompletions = dailyCompletions,
                    streakHeatmapData = heatmapData,
                    onTimeCompletions = onTime,
                    overdueCompletions = overdue,
                    bestDay = bestDay,
                    bestWeek = bestWeek,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
