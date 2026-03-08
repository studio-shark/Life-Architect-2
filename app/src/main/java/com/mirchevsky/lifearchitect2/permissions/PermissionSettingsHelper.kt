package com.mirchevsky.lifearchitect2.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * PermissionSettingsHelper
 * ─────────────────────────────────────────────────────────────────────────────
 * Opens the system App Info / Permissions page for Life Architect 2 directly,
 * scoped to this app only. Used as the last resort when Android has permanently
 * blocked a permission from being re-requested via the system dialog.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/permissions/PermissionSettingsHelper.kt
 */
fun openAppPermissionSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
