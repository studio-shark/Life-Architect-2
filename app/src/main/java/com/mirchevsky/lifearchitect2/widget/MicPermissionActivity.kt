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
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.mirchevsky.lifearchitect2.permissions.PermissionGateState
import com.mirchevsky.lifearchitect2.permissions.PermissionPrefs
import com.mirchevsky.lifearchitect2.permissions.openAppPermissionSettings
import com.mirchevsky.lifearchitect2.permissions.resolvePermissionGateState

/**
 * MicPermissionActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * A transparent, zero-UI trampoline Activity that requests RECORD_AUDIO on
 * behalf of [WidgetOverlayService] using the full [PermissionGateState] logic:
 *
 * - **Not yet requested / requestable** → shows the system permission dialog.
 * - **Denied once (rationale)** → shows an in-app [AlertDialog] explaining why,
 *   then re-launches the system dialog if the user taps "Allow".
 * - **Permanently denied** → shows a "blocked" [AlertDialog] with an
 *   "Open Settings" button that deep-links to the app's permission settings.
 *
 * The Activity finishes as soon as the user makes a choice in any dialog.
 *
 * Declared in AndroidManifest.xml with:
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *   android:excludeFromRecents="true"
 *   android:exported="false"
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/MicPermissionActivity.kt
 */
class MicPermissionActivity : ComponentActivity() {

    private var showRationale by mutableStateOf(false)
    private var showBlocked   by mutableStateOf(false)

    private val requestAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            finish()
            return@registerForActivityResult
        }
        // Re-resolve to determine whether Android will show the dialog again
        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.RECORD_AUDIO, prefs)) {
            PermissionGateState.RequestableWithRationale -> showRationale = true
            PermissionGateState.PermanentlyDenied        -> showBlocked   = true
            else                                         -> finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }

        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.RECORD_AUDIO, prefs)) {
            PermissionGateState.Granted -> finish()

            PermissionGateState.RequestableFirstTime,
            PermissionGateState.RequestableWithRationale -> {
                prefs.markRequested(Manifest.permission.RECORD_AUDIO)
                requestAudio.launch(Manifest.permission.RECORD_AUDIO)
            }

            PermissionGateState.PermanentlyDenied -> {
                // Show blocked dialog immediately — system won't show the dialog
                setContent {
                    MaterialTheme {
                        if (showBlocked) {
                            AlertDialog(
                                onDismissRequest = { finish() },
                                title = { Text("Microphone access blocked") },
                                text  = { Text(
                                    "You have permanently denied microphone access. " +
                                            "To use voice input on the widget, open Settings → " +
                                            "Permissions → Microphone and enable it."
                                )},
                                confirmButton = {
                                    TextButton(onClick = {
                                        openAppPermissionSettings(this@MicPermissionActivity)
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
            }
        }

        // Rationale and blocked dialogs rendered via setContent after launch
        setContent {
            MaterialTheme {
                if (showRationale) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Microphone access needed") },
                        text  = { Text(
                            "Life Architect needs the microphone to transcribe your voice " +
                                    "into a task title. Tap \"Allow\" to grant access."
                        )},
                        confirmButton = {
                            TextButton(onClick = {
                                showRationale = false
                                val prefs2 = PermissionPrefs(this@MicPermissionActivity)
                                prefs2.markRequested(Manifest.permission.RECORD_AUDIO)
                                requestAudio.launch(Manifest.permission.RECORD_AUDIO)
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
                        title = { Text("Microphone access blocked") },
                        text  = { Text(
                            "You have permanently denied microphone access. " +
                                    "To use voice input on the widget, open Settings → " +
                                    "Permissions → Microphone and enable it."
                        )},
                        confirmButton = {
                            TextButton(onClick = {
                                openAppPermissionSettings(this@MicPermissionActivity)
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
