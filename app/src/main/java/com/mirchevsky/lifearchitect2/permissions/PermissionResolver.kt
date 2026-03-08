package com.mirchevsky.lifearchitect2.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * PermissionResolver
 * ─────────────────────────────────────────────────────────────────────────────
 * Resolves the current [PermissionGateState] for a given permission by combining
 * Android's [shouldShowRequestPermissionRationale] with our own [PermissionPrefs]
 * request history — the only way to distinguish "never asked" from "permanently
 * denied", since both return false from the rationale API.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/permissions/PermissionResolver.kt
 */
fun resolvePermissionGateState(
    context: Context,
    permission: String,
    prefs: PermissionPrefs
): PermissionGateState {

    val granted = ContextCompat.checkSelfPermission(
        context, permission
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) return PermissionGateState.Granted

    val activity = context.findActivity()

    // No Activity in context (e.g. called from a Service): fall back to
    // conservative state based on whether we have asked before.
    if (activity == null) {
        return if (prefs.wasRequested(permission)) {
            PermissionGateState.PermanentlyDenied
        } else {
            PermissionGateState.RequestableFirstTime
        }
    }

    val wasRequestedBefore = prefs.wasRequested(permission)
    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, permission
    )

    return when {
        !wasRequestedBefore -> PermissionGateState.RequestableFirstTime
        shouldShowRationale -> PermissionGateState.RequestableWithRationale
        else               -> PermissionGateState.PermanentlyDenied
    }
}

/** Walks up the [ContextWrapper] chain to find the host [Activity], or null. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity       -> this
    is ContextWrapper -> baseContext.findActivity()
    else              -> null
}
