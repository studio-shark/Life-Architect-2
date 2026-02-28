package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.Bars
import io.lifephysics.architect2.ui.viewmodel.AnalyticsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The main Analytics screen composable.
 *
 * Displays a summary of the user's productivity data including total tasks,
 * current streak, total XP, a 14-day bar chart, a 90-day streak heatmap,
 * due date performance, and personal best records.
 *
 * @param viewModel The [AnalyticsViewModel] that provides the data for this screen.
 */
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatRow(
                        totalTasks = uiState.totalTasksCompleted,
                        streak = uiState.currentStreak,
                        totalXp = uiState.totalXp
                    )
                }
                item { CompletionChart(data = uiState.dailyCompletions) }
                item { StreakHeatmap(data = uiState.streakHeatmapData) }
                item {
                    CalendarAnalytics(
                        onTime = uiState.onTimeCompletions,
                        overdue = uiState.overdueCompletions
                    )
                }
                item {
                    PersonalBests(
                        bestDay = uiState.bestDay,
                        bestWeek = uiState.bestWeek
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stat Row
// ---------------------------------------------------------------------------

@Composable
private fun StatRow(totalTasks: Int, streak: Int, totalXp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Tasks Done", value = totalTasks.toString(), modifier = Modifier.weight(1f))
        StatCard(title = "Streak", value = "$streak days", modifier = Modifier.weight(1f))
        StatCard(title = "Total XP", value = totalXp.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Completion Chart
// ---------------------------------------------------------------------------

@Composable
private fun CompletionChart(data: Map<LocalDate, Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Task Completions — Last 14 Days",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.values.any { it > 0 }) {
                val sortedEntries = data.entries.sortedBy { it.key }
                val barColor = MaterialTheme.colorScheme.primary

                ColumnChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    data = sortedEntries.map { (date, count) ->
                        Bars(
                            label = date.format(DateTimeFormatter.ofPattern("d")),
                            values = listOf(
                                Bars.Data(
                                    value = count.toDouble(),
                                    color = SolidColor(barColor)
                                )
                            )
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No completed tasks yet.\nStart completing tasks to see your progress!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Streak Heatmap
// ---------------------------------------------------------------------------

@Composable
private fun StreakHeatmap(data: Set<LocalDate>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity — Last 90 Days",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(18),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(100.dp),
                userScrollEnabled = false
            ) {
                val startDate = LocalDate.now().minusDays(89)
                items(90) { i ->
                    val date = startDate.plusDays(i.toLong())
                    val isActive = data.contains(date)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Calendar Analytics
// ---------------------------------------------------------------------------

@Composable
private fun CalendarAnalytics(onTime: Int, overdue: Int) {
    if (onTime == 0 && overdue == 0) return // Hide section if no due-date data

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Due Date Performance",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "On Time", value = onTime.toString(), modifier = Modifier.weight(1f))
                StatCard(title = "Overdue", value = overdue.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Personal Bests
// ---------------------------------------------------------------------------

@Composable
private fun PersonalBests(
    bestDay: Pair<LocalDate, Int>?,
    bestWeek: Pair<LocalDate, Int>?
) {
    if (bestDay == null && bestWeek == null) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Personal Bests",
                style = MaterialTheme.typography.titleMedium
            )
            bestDay?.let { (date, count) ->
                Text(
                    text = "Best day: $count tasks on ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            bestWeek?.let { (weekStart, count) ->
                Text(
                    text = "Best week: $count tasks (week of ${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
