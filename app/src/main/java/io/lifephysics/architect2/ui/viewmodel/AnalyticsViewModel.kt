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

enum class DayStatus {
    PENDING, COMPLETED, BOTH
}

data class AnalyticsUiState(
    val totalTasksCompleted: Int = 0,
    val totalXp: Int = 0,
    val dailyCompletions: Map<LocalDate, Int> = emptyMap(),
    val monthlyTaskStatus: Map<LocalDate, DayStatus> = emptyMap(),
    val onTimeCompletions: Int = 0,
    val overdueCompletions: Int = 0,
    val bestDay: Pair<LocalDate, Int>? = null,
    val bestWeek: Pair<LocalDate, Int>? = null,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                repository.getUser(),
                repository.getCompletedTasks(),
                repository.observePendingTasksForUser("local_user")
            ) { user, completedTasks, pendingTasks ->
                if (user == null) return@combine AnalyticsUiState(isLoading = false)

                val zone = ZoneId.systemDefault()
                val now = LocalDate.now()
                val weekFields = WeekFields.of(Locale.getDefault())

                val tasksByDate = completedTasks
                    .filter { it.completedAt != null }
                    .groupBy { task ->
                        Instant.ofEpochMilli(task.completedAt!!).atZone(zone).toLocalDate()
                    }

                val dailyCompletions = (0..13).associate { offset ->
                    val day = now.minusDays(offset.toLong())
                    day to (tasksByDate[day]?.size ?: 0)
                }

                val bestDay = tasksByDate
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }

                val weeklyCompletions = completedTasks
                    .filter { it.completedAt != null }
                    .groupBy { task ->
                        val date = Instant.ofEpochMilli(task.completedAt!!).atZone(zone).toLocalDate()
                        date.with(weekFields.dayOfWeek(), 1)
                    }
                val bestWeek = weeklyCompletions
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }

                val onTime = completedTasks.count {
                    it.dueDate != null && it.completedAt != null && it.completedAt <= it.dueDate
                }
                val overdue = completedTasks.count {
                    it.dueDate != null && it.completedAt != null && it.completedAt > it.dueDate
                }

                val completedDays = completedTasks
                    .filter { it.completedAt != null }
                    .map { Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
                    .toSet()

                val pendingDays = pendingTasks
                    .filter { it.dueDate != null }
                    .map { Instant.ofEpochMilli(it.dueDate!!).atZone(zone).toLocalDate() }
                    .toSet()

                val allDays = completedDays + pendingDays
                val monthlyStatus = allDays.associateWith { date ->
                    val hasCompleted = completedDays.contains(date)
                    val hasPending = pendingDays.contains(date)
                    when {
                        hasCompleted && hasPending -> DayStatus.BOTH
                        hasCompleted -> DayStatus.COMPLETED
                        else -> DayStatus.PENDING
                    }
                }

                AnalyticsUiState(
                    totalTasksCompleted = completedTasks.size,
                    totalXp = user.totalXp,
                    dailyCompletions = dailyCompletions,
                    monthlyTaskStatus = monthlyStatus,
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
