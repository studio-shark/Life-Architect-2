package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.ui.viewmodel.AnalyticsViewModel
import io.lifephysics.architect2.ui.viewmodel.DayStatus
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The main Analytics screen composable.
 *
 * Displays a summary of the user's productivity data including total tasks,
 * total XP, a 14-day bar chart, a swipeable monthly calendar with green/red
 * task dots, due date performance, and personal best records.
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
                        totalXp = uiState.totalXp
                    )
                }
                item { CompletionChart(data = uiState.dailyCompletions) }
                item { MonthlyTaskCalendar(monthlyTaskStatus = uiState.monthlyTaskStatus) }
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
private fun StatRow(totalTasks: Int, totalXp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Tasks Done", value = totalTasks.toString(), modifier = Modifier.weight(1f))
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
            val sortedEntries = data.entries
                .sortedBy { it.key }
                .takeLast(14)
            val barColor = MaterialTheme.colorScheme.primary
            if (sortedEntries.any { it.value > 0 }) {
                ColumnChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
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
                    },
                    barProperties = BarProperties(thickness = 12.dp)
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
// Monthly Task Calendar (pure Compose, no external library)
// ---------------------------------------------------------------------------
@Composable
private fun MonthlyTaskCalendar(monthlyTaskStatus: Map<LocalDate, DayStatus>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    enabled = currentMonth.isBefore(YearMonth.now())
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Day-of-week headers (Mon–Sun)
            val dayHeaders = DayOfWeek.values().map {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Build the grid cells
            val firstDayOfMonth = currentMonth.atDay(1)
            // Offset so Monday = 0
            val startOffset = (firstDayOfMonth.dayOfWeek.value - 1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            // Round up to full weeks
            val gridSize = if (totalCells % 7 == 0) totalCells else totalCells + (7 - totalCells % 7)

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((gridSize / 7) * 52).dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(gridSize) { index ->
                    if (index < startOffset || index >= startOffset + daysInMonth) {
                        // Empty cell
                        Box(modifier = Modifier.size(40.dp))
                    } else {
                        val dayNumber = index - startOffset + 1
                        val date = currentMonth.atDay(dayNumber)
                        val isToday = date == today
                        val status = monthlyTaskStatus[date]

                        Column(
                            modifier = Modifier.size(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Day number
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .then(
                                        if (isToday) Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isToday)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Task dot(s)
                            if (status != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (status == DayStatus.COMPLETED || status == DayStatus.BOTH) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50)) // green
                                        )
                                    }
                                    if (status == DayStatus.PENDING || status == DayStatus.BOTH) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF44336)) // red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Due Date Performance
// ---------------------------------------------------------------------------
@Composable
private fun CalendarAnalytics(onTime: Int, overdue: Int) {
    if (onTime == 0 && overdue == 0) return
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
