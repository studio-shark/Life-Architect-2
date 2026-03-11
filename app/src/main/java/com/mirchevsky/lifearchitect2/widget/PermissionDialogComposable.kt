package com.mirchevsky.lifearchitect2.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * PermissionDialog
 * ─────────────────────────────────────────────────────────────────────────────
 * A shared, reusable [AlertDialog] composable used by [CalendarPermissionActivity]
 * and [MicPermissionActivity] to show rationale and permanently-denied dialogs.
 *
 * Extracted from duplicated inline AlertDialog blocks to keep the permission
 * activities clean and consistent.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/PermissionDialogComposable.kt
 */
@Composable
fun PermissionDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Open Settings",
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    )
}
