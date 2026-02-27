package io.lifephysics.architect2.ui.screens

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lifephysics.architect2.ui.composables.AddTaskItem
import io.lifephysics.architect2.ui.composables.TaskItem
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

/**
 * The root composable for the Tasks screen.
 * Shows only pending tasks. Completed tasks move to the History tab.
 * An inline [AddTaskItem] row at the bottom allows adding new tasks
 * and opening Google Calendar.
 */
@Composable
fun TasksScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.pendingTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onCompleted = { viewModel.onTaskCompleted(task) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                AddTaskItem(
                    onAddTask = { title -> viewModel.onAddTask(title) },
                    onAddToCalendar = { title ->
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, title)
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
