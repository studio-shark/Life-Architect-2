package com.mirchevsky.lifearchitect2.widget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * A transparent, zero-UI trampoline Activity that requests READ_CALENDAR and
 * WRITE_CALENDAR on behalf of [WidgetOverlayService] using the full
 * [PermissionGateState] logic — identical in structure to [MicPermissionActivity]:
 *
 * - **Not yet requested / requestable** → shows the system permission dialog.
 * - **Denied once (rationale)** → shows an in-app [AlertDialog] explaining why,
 *   then re-launches the system dialog if the user taps "Allow".
 * - **Permanently denied** → shows a "blocked" [AlertDialog] with an
 *   "Open Settings" button that deep-links to the app's permission settings.
 *
 * Both READ_CALENDAR and WRITE_CALENDAR are requested together. READ_CALENDAR is
 * used as the representative permission for [resolvePermissionGateState] since
 * both permissions share the same Android permission group and always have the
 * same grant state.
 *
 * Declared in AndroidManifest.xml with:
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *   android:excludeFromRecents="true"
 *   android:exported="false"
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/CalendarPermissionActivity.kt
 */
class CalendarPermissionActivity : ComponentActivity() {

    private var showRationale by mutableStateOf(false)
    private var showBlocked   by mutableStateOf(false)

    private val requestCalendar = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted  = results[Manifest.permission.READ_CALENDAR]  == true
        val writeGranted = results[Manifest.permission.WRITE_CALENDAR] == true

        if (readGranted && writeGranted) {
            finish()
            return@registerForActivityResult
        }

        // Re-resolve using READ_CALENDAR as the representative permission
        // (both are in the same permission group so their state is always identical).
        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.READ_CALENDAR, prefs)) {
            PermissionGateState.RequestableWithRationale -> showRationale = true
            PermissionGateState.PermanentlyDenied        -> showBlocked   = true
            else                                         -> finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val readGranted  = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (readGranted && writeGranted) {
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
                // Show blocked dialog immediately — system won't show the dialog again.
                // setContent is called here so the dialog renders before showBlocked is set.
                setContent {
                    MaterialTheme {
                        if (showBlocked) {
                            AlertDialog(
                                onDismissRequest = { finish() },
                                title = { Text("Calendar access blocked") },
                                text  = { Text(
                                    "You have permanently denied calendar access. " +
                                    "To create events from the widget, open Settings → " +
                                    "Permissions → Calendar and enable it."
                                )},
                                confirmButton = {
                                    TextButton(onClick = {
                                        openAppPermissionSettings(this@CalendarPermissionActivity)
                                        finish()
                                    }) { Text("Open Settings") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { finish() }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
                showBlocked = true
                return
            }
        }

        // Rationale and blocked dialogs rendered via setContent after the permission launch.
        setContent {
            MaterialTheme {
                if (showRationale) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Calendar access needed") },
                        text  = { Text(
                            "Life Architect needs calendar access to create events " +
                            "directly from the widget. Tap \"Allow\" to grant access."
                        )},
                        confirmButton = {
                            TextButton(onClick = {
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
                            }) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text("Not now") }
                        }
                    )
                }

                if (showBlocked) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Calendar access blocked") },
                        text  = { Text(
                            "You have permanently denied calendar access. " +
                            "To create events from the widget, open Settings → " +
                            "Permissions → Calendar and enable it."
                        )},
                        confirmButton = {
                            TextButton(onClick = {
                                openAppPermissionSettings(this@CalendarPermissionActivity)
                                finish()
                            }) { Text("Open Settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
