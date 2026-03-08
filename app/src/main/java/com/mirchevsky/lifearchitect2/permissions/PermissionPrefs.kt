package com.mirchevsky.lifearchitect2.permissions

import android.content.Context

/**
 * PermissionPrefs
 * ─────────────────────────────────────────────────────────────────────────────
 * Lightweight SharedPreferences wrapper that tracks whether a given permission
 * has ever been requested. This is required because Android's
 * [shouldShowRequestPermissionRationale] returns false both before the first
 * request AND after "Don't ask again" — we need this flag to distinguish them.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/permissions/PermissionPrefs.kt
 */
class PermissionPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(
        "la2_permission_gate_prefs",
        Context.MODE_PRIVATE
    )

    /** Mark that [permission] has been requested at least once. */
    fun markRequested(permission: String) {
        prefs.edit().putBoolean("requested_$permission", true).apply()
    }

    /** Returns true if [permission] has been requested at least once before. */
    fun wasRequested(permission: String): Boolean =
        prefs.getBoolean("requested_$permission", false)
}
