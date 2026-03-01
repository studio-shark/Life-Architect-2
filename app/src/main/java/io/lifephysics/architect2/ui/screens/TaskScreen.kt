package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lifephysics.architect2.ui.composables.AddTaskItem
import io.lifephysics.architect2.ui.composables.TaskItem
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

/**
 * The root composable for the Tasks screen.
 *
 * Shows only pending tasks in a [LazyColumn]. Completed tasks move to the History tab.
 * [AddTaskItem] is pinned at the bottom with smart date-intent detection.
 *
 * When [focusAddTask] is true (triggered by the + shortcut tab in the bottom nav),
 * the [AddTaskItem] text field requests focus automatically and [onFocusHandled] is
 * called to reset the flag.
 *
 * @param viewModel The [MainViewModel] that provides task data and handles actions.
 * @param focusAddTask When true, automatically focuses the add-task field.
 * @param onFocusHandled Called after the focus request fires so the caller can reset the flag.
 */
@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    focusAddTask: Boolean = false,
    onFocusHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp)
    ) {
        // Title pinned at top
        Text(
            text = "Tasks",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Task list fills remaining space
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.pendingTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onCompleted = { viewModel.onTaskCompleted(task) }
                )
            }
        }

        // AddTaskItem pinned at bottom
        AddTaskItem(
            onAddTask = { title, difficulty, dueDate ->
                viewModel.onAddTask(title, difficulty, dueDate)
            },
            requestFocus = focusAddTask,
            onFocusConsumed = onFocusHandled
        )
    }
}
