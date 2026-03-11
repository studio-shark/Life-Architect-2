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
 * MicPermissionActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * Requests RECORD_AUDIO on behalf of [WidgetOverlayService].
 *
 * Key change from the original: on a successful grant, this activity now broadcasts
 * [WidgetOverlayService.ACTION_PERMISSION_GRANTED] before finishing. The service
 * listens for this broadcast and immediately re-executes the pending intent (e.g.
 * ACTION_MIC), so the voice panel opens automatically without requiring a second tap.
 */
class MicPermissionActivity : ComponentActivity() {

    private var showRationale by mutableStateOf(false)
    private var showBlocked by mutableStateOf(false)

    private val requestAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Notify the service so it can resume the pending action immediately.
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
            finish()
            return@registerForActivityResult
        }

        val prefs = PermissionPrefs(this)
        when (resolvePermissionGateState(this, Manifest.permission.RECORD_AUDIO, prefs)) {
            PermissionGateState.RequestableWithRationale -> showRationale = true
            PermissionGateState.PermanentlyDenied -> showBlocked = true
            else -> finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
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
                setContent {
                    MaterialTheme {
                        if (showBlocked) {
                            PermissionDialog(
                                title = "Microphone access blocked",
                                text = "You have permanently denied microphone access. " +
                                        "To use voice input on the widget, open Settings → " +
                                        "Permissions → Microphone and enable it.",
                                onConfirm = {
                                    openAppPermissionSettings(this@MicPermissionActivity)
                                    finish()
                                },
                                onDismiss = { finish() }
                            )
                        }
                    }
                }
                showBlocked = true
            }
        }

        setContent {
            MaterialTheme {
                if (showRationale) {
                    PermissionDialog(
                        title = "Microphone access needed",
                        text = "Life Architect needs the microphone to transcribe your voice " +
                                "into a task title. Tap \"Allow\" to grant access.",
                        onConfirm = {
                            showRationale = false
                            val prefs2 = PermissionPrefs(this@MicPermissionActivity)
                            prefs2.markRequested(Manifest.permission.RECORD_AUDIO)
                            requestAudio.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onDismiss = { finish() },
                        confirmText = "Allow",
                        dismissText = "Not now"
                    )
                }

                if (showBlocked) {
                    PermissionDialog(
                        title = "Microphone access blocked",
                        text = "You have permanently denied microphone access. " +
                                "To use voice input on the widget, open Settings → " +
                                "Permissions → Microphone and enable it.",
                        onConfirm = {
                            openAppPermissionSettings(this@MicPermissionActivity)
                            finish()
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}
