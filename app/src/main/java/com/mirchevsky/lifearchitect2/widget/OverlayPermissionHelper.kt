package com.mirchevsky.lifearchitect2.widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * OverlayPermissionDialogActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * A transparent, zero-chrome trampoline Activity that explains the
 * "Display over other apps" permission and sends the user to Settings.
 *
 * Key change from the original: [onResume] now checks if the user has just
 * returned from the Settings screen with the permission granted. If so, it
 * broadcasts [WidgetOverlayService.ACTION_PERMISSION_GRANTED] before finishing,
 * which causes the service to immediately re-execute the pending widget action
 * (e.g. open the Add Task panel) without requiring a second tap.
 */
class OverlayPermissionDialogActivity : ComponentActivity() {

    private var showDialog by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already granted (e.g. re-launched after coming back from Settings),
        // signal the service and finish immediately.
        if (Settings.canDrawOverlays(this)) {
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                if (showDialog) {
                    Dialog(onDismissRequest = { finish() }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "One quick step",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "To add tasks directly from your home screen, " +
                                            "Life Architect needs permission to show a small " +
                                            "panel over other apps.\n\n" +
                                            "Tap \"Take me there\" — you\'ll see a single toggle " +
                                            "labelled \"Allow display over other apps\". " +
                                            "Switch it on, then come back.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        showDialog = false
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                        // Do NOT finish here — onResume will detect the grant
                                        // and broadcast ACTION_PERMISSION_GRANTED.
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = "Take me there",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = { finish() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Not now",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when the user returns from the system Settings screen.
     * If the overlay permission has been granted, broadcast the signal so the
     * service can resume the pending widget action immediately.
     */
    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            sendBroadcast(Intent(WidgetOverlayService.ACTION_PERMISSION_GRANTED))
            finish()
        }
    }
}
