package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

/**
 * Displays a single task row with a checkbox and title.
 * Clicking anywhere on the row or the checkbox triggers [onTaskClicked].
 */
@Composable
fun TaskItem(
    task: TaskEntity,
    onTaskClicked: (TaskEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClicked(task) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onTaskClicked(task) }
        )
        Text(
            text = task.title,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/**
 * Displays the full scrollable list of tasks using a performant LazyColumn.
 * Routes completed/incomplete toggle events up to the [MainViewModel].
 */
@Composable
fun TaskList(
    tasks: List<TaskEntity>,
    viewModel: MainViewModel
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task = task) {
                if (task.isCompleted) {
                    viewModel.onTaskReverted(task)
                } else {
                    viewModel.onTaskCompleted(task)
                }
            }
        }
    }
}

/**
 * The root composable for the Tasks screen.
 * Collects the UI state from the [MainViewModel] and passes the task list
 * down to [TaskList] for rendering.
 */
@Composable
fun TasksScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    TaskList(tasks = uiState.tasks, viewModel = viewModel)
}
