package io.lifephysics.architect2.ui.composables

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.data.db.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a single pending task in a card.
 *
 * If the task has a [TaskEntity.dueDate], a formatted date label is shown below the
 * title and a calendar icon appears on the right. Tapping the calendar icon opens the
 * default calendar app to the day of the event so the user can view the existing event.
 *
 * Tapping anywhere else on the card triggers [onCompleted].
 *
 * @param task The task entity to display.
 * @param onCompleted Callback invoked when the card or checkbox is tapped.
 */
@Composable
fun TaskItem(
    task: TaskEntity,
    onCompleted: (TaskEntity) -> Unit
) {
    val context = LocalContext.current
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
                IconButton(onClick = {
                    // Open the default calendar app to the day of the event so the
                    // user can view the existing event — not create a new one.
                    val epochMillis = task.dueDate ?: return@IconButton
                    val uri = Uri.parse("content://com.android.calendar/time/$epochMillis")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, epochMillis)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, epochMillis + 3_600_000L)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "View in Calendar",
                        tint = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
