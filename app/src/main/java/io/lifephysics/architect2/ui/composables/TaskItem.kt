package io.lifephysics.architect2.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.data.db.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a single pending task in a card that visually matches the TrendCard style.
 *
 * If the task has a [TaskEntity.dueDate], a formatted date label is shown below the
 * title and a calendar icon appears on the right. Tapping the calendar icon fires
 * [onCalendarClick], which opens the system default calendar app at the due date.
 *
 * Tapping anywhere else on the card triggers [onCompleted].
 *
 * @param task The task entity to display.
 * @param onCompleted Callback invoked when the card or checkbox is tapped.
 * @param onCalendarClick Callback invoked when the calendar icon is tapped.
 */
@Composable
fun TaskItem(
    task: TaskEntity,
    onCompleted: (TaskEntity) -> Unit,
    onCalendarClick: (TaskEntity) -> Unit
) {
    val dueDate: LocalDate? = task.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val isOverdue = dueDate != null && dueDate.isBefore(LocalDate.now())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCompleted(task) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = null, // Handled by the card's clickable modifier
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dueDate != null) {
                    val label = dueDate.format(DateTimeFormatter.ofPattern("MMM d"))
                    Text(
                        text = if (isOverdue) "Overdue — $label" else "Due $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (dueDate != null) {
                IconButton(onClick = { onCalendarClick(task) }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Open in Calendar",
                        tint = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
