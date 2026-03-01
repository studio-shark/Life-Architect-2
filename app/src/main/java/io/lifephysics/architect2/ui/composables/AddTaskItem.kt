package io.lifephysics.architect2.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.utils.DateIntentParser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The inline add-task row shown at the bottom of the Tasks screen.
 *
 * Behaviour:
 * - The text field shows "New task..." as a placeholder.
 * - The '+' button is greyed out when the field is empty and turns the primary
 *   colour (green) as soon as the user starts typing.
 * - When [DateIntentParser] detects a date or time pattern in the text (debounced
 *   300ms), a calendar icon slides in to the left of the '+' button. Tapping it
 *   opens a [DatePickerDialog] with the parser's best guess pre-filled.
 * - If the parser result is confident the icon is fully opaque; a weak (time-only)
 *   match dims the icon to 40% alpha.
 * - Pressing the IME Done action or tapping '+' submits the task, dismisses the
 *   keyboard, and resets the row.
 * - When [requestFocus] is true the text field requests focus immediately, which
 *   opens the keyboard. The caller must reset the flag via [onFocusConsumed] to
 *   avoid re-triggering on recomposition.
 *
 * @param onAddTask Called with the task title, difficulty string, and optional due
 *   date when the user confirms.
 * @param requestFocus When true, the text field requests focus on first composition.
 * @param onFocusConsumed Called once after the focus request fires so the caller
 *   can reset the flag.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AddTaskItem(
    onAddTask: (title: String, difficulty: String, dueDate: LocalDateTime?) -> Unit,
    requestFocus: Boolean = false,
    onFocusConsumed: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<DateIntentParser.ParseResult?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmedDueDate by remember { mutableStateOf<LocalDateTime?>(null) }

    val titleFlow = remember { MutableStateFlow("") }

    // Request focus when the '+' shortcut tab is tapped from another screen
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusConsumed()
        }
    }

    // Debounce the title input and run the date parser on each change
    LaunchedEffect(Unit) {
        titleFlow
            .debounce(300)
            .collect { text ->
                parseResult = if (text.isNotBlank()) DateIntentParser.parse(text) else null
            }
    }

    val hasText = title.isNotBlank()
    val showCalendar = hasText && (confirmedDueDate != null || parseResult?.bestGuess != null)

    fun submit() {
        if (!hasText) return
        val dueDate = confirmedDueDate ?: parseResult?.bestGuess
        onAddTask(title.trim(), "MEDIUM", dueDate)
        title = ""
        titleFlow.value = ""
        parseResult = null
        confirmedDueDate = null
        keyboardController?.hide()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                titleFlow.value = it
                confirmedDueDate = null
            },
            placeholder = { Text("New task...") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Calendar icon — slides in from the right when date-intent is detected
        AnimatedVisibility(
            visible = showCalendar,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            val isConfident = confirmedDueDate != null || parseResult?.isConfident == true
            FilledIconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF7C4DFF).copy(
                        alpha = if (isConfident) 1f else 0.4f
                    ),
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Set Due Date"
                )
            }
        }

        if (showCalendar) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // '+' button — greyed out when empty, primary colour when there is text
        FilledIconButton(
            onClick = { submit() },
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (hasText) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                contentColor = if (hasText) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            enabled = hasText
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task"
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val initialMillis = (confirmedDueDate ?: parseResult?.bestGuess)
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        confirmedDueDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
