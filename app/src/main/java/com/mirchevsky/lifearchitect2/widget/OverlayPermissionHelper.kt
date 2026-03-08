package com.mirchevsky.lifearchitect2.widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * OverlayPermissionDialogActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * A transparent, zero-chrome trampoline Activity that shows a friendly in-app
 * dialog explaining the "Display over other apps" permission before sending the
 * user to the Settings deep-link.
 *
 * Android does not provide a system permission dialog for SYSTEM_ALERT_WINDOW —
 * it always requires the user to navigate to Settings manually. This Activity
 * bridges the gap by:
 *   1. Showing a warm, plain-language explanation of what the permission does
 *      and exactly what the user will see in Settings.
 *   2. Providing a single "Take me there" button that opens the app's own
 *      overlay toggle page directly (not the full list of apps).
 *   3. Providing a "Not now" option that dismisses without navigating.
 *
 * Launched by [WidgetOverlayService] when [Settings.canDrawOverlays] returns
 * false, instead of jumping straight to Settings.
 *
 * Declared in AndroidManifest.xml with:
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *   android:excludeFromRecents="true"
 *   android:exported="false"
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/OverlayPermissionDialogActivity.kt
 */
class OverlayPermissionDialogActivity : ComponentActivity() {

    private var showDialog by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If permission was already granted (e.g. user came back from Settings),
        // finish immediately — nothing to show.
        if (Settings.canDrawOverlays(this)) {
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
                                // Icon
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                // Title
                                Text(
                                    text = "One quick step",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(10.dp))

                                // Explanation
                                Text(
                                    text = "To add tasks directly from your home screen, " +
                                            "Life Architect needs permission to show a small " +
                                            "panel over other apps.\n\n" +
                                            "Tap \"Take me there\" — you'll see a single toggle " +
                                            "labelled \"Allow display over other apps\". " +
                                            "Switch it on, then come back.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )

                                Spacer(Modifier.height(24.dp))

                                // Primary action
                                Button(
                                    onClick = {
                                        showDialog = false
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                        finish()
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

                                // Secondary action
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
}
