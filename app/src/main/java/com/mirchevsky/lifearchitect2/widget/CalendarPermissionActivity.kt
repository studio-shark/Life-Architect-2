package com.mirchevsky.lifearchitect2.widget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.mirchevsky.lifearchitect2.permissions.PermissionGateState
import com.mirchevsky.lifearchitect2.permissions.PermissionPrefs
import com.mirchevsky.lifearchitect2.permissions.openAppPermissionSettings
import com.mirchevsky.lifearchitect2.permissions.resolvePermissionGateState

/**
 * CalendarPermissionActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * Requests READ_CALENDAR and WRITE_CALENDAR on behalf of [WidgetOverlayService].
 *
 * Key change from the original: on a successful grant, this activity now broadcasts
 * [WidgetOverlayService.ACTION_PERMISSION_GRANTED] before finishing. The service
 * listens for this broadcast and immediately re-executes the pending intent (e.g.
 * ACTION_ADD_EVENT), so the user is taken directly into the panel without needing
 * to tap the widget button again.
 */
class CalendarPermissionActivity : ComponentActivity() {

    private var showRationale by mutableStateOf(false)
    private var showBlocked by mutableStateOf(false)

    private val requestCalendar = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted = results[Manifest.permission.READ_CALENDAR] == true
        val writeGranted = results[Manifest.permission.WRITE_CALENDAR] == true

        if (readGranted && writeGranted) {
            // Notify the service so it can resume the pending action immediately.
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
            finish()
            return@registerForActivityResult
        }

        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.READ_CALENDAR, prefs)) {
            PermissionGateState.RequestableWithRationale -> showRationale = true
            PermissionGateState.PermanentlyDenied -> showBlocked = true
            else -> finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val readGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (readGranted && writeGranted) {
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
            finish()
            return
        }

        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.READ_CALENDAR, prefs)) {
            PermissionGateState.Granted -> finish()

            PermissionGateState.RequestableFirstTime,
            PermissionGateState.RequestableWithRationale -> {
                prefs.markRequested(Manifest.permission.READ_CALENDAR)
                prefs.markRequested(Manifest.permission.WRITE_CALENDAR)
                requestCalendar.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            }

            PermissionGateState.PermanentlyDenied -> {
                setContent {
                    MaterialTheme {
                        if (showBlocked) {
                            PermissionDialog(
                                title = "Calendar access blocked",
                                text = "You have permanently denied calendar access. " +
                                        "To create events from the widget, open Settings → " +
                                        "Permissions → Calendar and enable it.",
                                onConfirm = {
                                    openAppPermissionSettings(this@CalendarPermissionActivity)
                                    finish()
                                },
                                onDismiss = { finish() }
                            )
                        }
                    }
                }
                showBlocked = true
                return
            }
        }

        setContent {
            MaterialTheme {
                if (showRationale) {
                    PermissionDialog(
                        title = "Calendar access needed",
                        text = "Life Architect needs calendar access to create events " +
                                "directly from the widget. Tap \"Allow\" to grant access.",
                        onConfirm = {
                            showRationale = false
                            val prefs2 = PermissionPrefs(this@CalendarPermissionActivity)
                            prefs2.markRequested(Manifest.permission.READ_CALENDAR)
                            prefs2.markRequested(Manifest.permission.WRITE_CALENDAR)
                            requestCalendar.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        onDismiss = { finish() },
                        confirmText = "Allow",
                        dismissText = "Not now"
                    )
                }

                if (showBlocked) {
                    PermissionDialog(
                        title = "Calendar access blocked",
                        text = "You have permanently denied calendar access. " +
                                "To create events from the widget, open Settings → " +
                                "Permissions → Calendar and enable it.",
                        onConfirm = {
                            openAppPermissionSettings(this@CalendarPermissionActivity)
                            finish()
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}
