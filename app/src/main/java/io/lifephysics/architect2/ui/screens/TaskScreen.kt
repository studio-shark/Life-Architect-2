package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Shows pending tasks in a [LazyColumn] followed by the inline [AddTaskItem] row.
 * The [AddTaskItem] row contains the text field, a smart calendar icon that appears
 * when date-intent is detected, and the '+' submit button.
 *
 * @param viewModel The [MainViewModel] that provides task data and handles actions.
 */
@Composable
fun TasksScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.pendingTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onCompleted = { viewModel.onTaskCompleted(task) },
                        onCalendarClick = { viewModel.onCalendarClick(it) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Inline add-task row — always visible at the bottom of the screen
            AddTaskItem(
                onAddTask = { title, difficulty, dueDate ->
                    viewModel.onAddTask(title, difficulty, dueDate)
                }
            )
        }
    }
}
