package io.lifephysics.architect2.ui.composables

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.lifephysics.architect2.data.db.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a single pending task in a card.
 *
 * Interaction model:
 * - **Tap the card** (anywhere except the title when not editing) → marks task complete.
 * - **Long-press the card** OR **tap the title text** → activates inline title editing.
 *   The title becomes a [BasicTextField] in-place; pressing Done / Enter (or tapping away)
 *   commits the change via [onUpdate]. No popup dialog is shown.
 * - **Pin icon** (filled amber when pinned, outlined default when not) → toggles [TaskEntity.isPinned].
 * - **Three-dot menu** → Edit date & time | Mark urgent / Remove urgent.
 *
 * Title colour priority: urgent (#E53935 red) > pinned (#FFC107 amber) > default.
 *
 * @param task The task entity to display.
 * @param onCompleted Callback when the task is checked off.
 * @param onUpdate Callback when the task is modified (pin, urgent, title, due-date).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onCompleted: (TaskEntity) -> Unit,
    onUpdate: (TaskEntity) -> Unit
) {
    val context = LocalContext.current
    val dueDate: LocalDate? = task.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val isOverdue = dueDate != null && dueDate.isBefore(LocalDate.now())

    // ── Inline editing state ────────────────────────────────────────────────
    var isEditing by remember { mutableStateOf(false) }
    // TextFieldValue keeps cursor at end when editing starts
    var titleFieldValue by remember(task.id) {
        mutableStateOf(TextFieldValue(task.title, selection = TextRange(task.title.length)))
    }
    val focusRequester = remember { FocusRequester() }

    // Request focus as soon as editing mode is entered
    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    fun commitEdit() {
        val newTitle = titleFieldValue.text.trim()
        if (newTitle.isNotBlank() && newTitle != task.title) {
            onUpdate(task.copy(title = newTitle))
        }
        isEditing = false
    }

    // ── Menu state ──────────────────────────────────────────────────────────
    var menuExpanded by remember { mutableStateOf(false) }

    // ── Edit date/time state ────────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember(task.id) { mutableStateOf(task.dueDate) }

    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = task.dueDate ?: todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        }
    )
    val existingHour = task.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour
    } ?: 9
    val existingMinute = task.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).minute
    } ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = existingHour,
        initialMinute = existingMinute,
        is24Hour = true
    )

    // ── Title colour ────────────────────────────────────────────────────────
    val titleColor: Color = when {
        task.isUrgent -> Color(0xFFE53935)
        task.isPinned -> Color(0xFFFFC107)
        else          -> MaterialTheme.colorScheme.onSurface
    }

    // ── Card ────────────────────────────────────────────────────────────────
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Long-press anywhere on the card activates inline editing;
                // single tap completes the task (only when not already editing).
                .combinedClickable(
                    onClick = { if (!isEditing) onCompleted(task) },
                    onLongClick = {
                        titleFieldValue = TextFieldValue(
                            task.title,
                            selection = TextRange(task.title.length)
                        )
                        isEditing = true
                    }
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = false,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Title area — switches between static Text and inline BasicTextField
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    BasicTextField(
                        value = titleFieldValue,
                        onValueChange = { titleFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = titleColor),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitEdit() })
                    )
                } else {
                    // Tap directly on the title text to start editing
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = titleColor,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                titleFieldValue = TextFieldValue(
                                    task.title,
                                    selection = TextRange(task.title.length)
                                )
                                isEditing = true
                            },
                            onLongClick = {
                                titleFieldValue = TextFieldValue(
                                    task.title,
                                    selection = TextRange(task.title.length)
                                )
                                isEditing = true
                            }
                        )
                    )
                }
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

            // Calendar icon — opens existing event in default calendar app
            if (dueDate != null) {
                IconButton(onClick = {
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

            // Pin icon — filled amber when pinned, outlined default when not
            IconButton(onClick = { onUpdate(task.copy(isPinned = !task.isPinned)) }) {
                Icon(
                    imageVector = if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (task.isPinned) "Unpin task" else "Pin task to top",
                    tint = if (task.isPinned) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Three-dot menu — Edit date & time | Mark urgent / Remove urgent
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Task options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // 1. Edit date & time
                    DropdownMenuItem(
                        text = { Text("Edit date & time") },
                        onClick = {
                            menuExpanded = false
                            showDatePicker = true
                        }
                    )
                    // 2. Mark urgent / Remove urgent
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (task.isUrgent) "Remove urgent" else "Mark urgent",
                                color = Color(0xFFE53935)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onUpdate(task.copy(isUrgent = !task.isUrgent))
                        }
                    )
                }
            }
        }
    }

    // ── Date picker dialog ──────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker dialog ──────────────────────────────────────────────────
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TimeInput(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            showTimePicker = false
                            val selectedDate = pendingDateMillis ?: return@TextButton
                            val localDate = Instant.ofEpochMilli(selectedDate)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            val localDateTime = LocalDateTime.of(
                                localDate,
                                java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                            val newMillis = localDateTime
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onUpdate(task.copy(dueDate = newMillis))
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }
}
