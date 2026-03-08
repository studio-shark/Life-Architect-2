package com.mirchevsky.lifearchitect2.permissions

/**
 * PermissionGateState
 * ─────────────────────────────────────────────────────────────────────────────
 * Represents the four possible states of a runtime permission in Life Architect 2.
 *
 * Used by [resolvePermissionGateState] and consumed by [PermissionGate] to
 * decide whether to request, explain, or hard-gate a feature.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/permissions/PermissionGateState.kt
 */
sealed interface PermissionGateState {
    /** Permission is granted — feature can proceed immediately. */
    data object Granted : PermissionGateState

    /** First time asking — launch the system dialog without any rationale. */
    data object RequestableFirstTime : PermissionGateState

    /**
     * User denied once — Android still allows re-requesting.
     * Show a brief in-app rationale dialog before re-launching the system dialog.
     */
    data object RequestableWithRationale : PermissionGateState

    /**
     * User tapped "Don't ask again" or denied twice — Android will no longer
     * show the system dialog. The only path forward is the app's Settings page.
     */
    data object PermanentlyDenied : PermissionGateState
}
