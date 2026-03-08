package com.mirchevsky.lifearchitect2.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mirchevsky.lifearchitect2.R
import com.mirchevsky.lifearchitect2.data.db.AppDatabase
import com.mirchevsky.lifearchitect2.data.db.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * WidgetOverlayService
 * ─────────────────────────────────────────────────────────────────────────────
 * A ForegroundService that draws a slide-up Jetpack Compose panel directly
 * onto the screen using WindowManager + TYPE_APPLICATION_OVERLAY.
 *
 * This is how the widget's +, mic, and calendar buttons achieve full text/voice/
 * calendar input without ever opening the main app.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/WidgetOverlayService.kt
 */
class WidgetOverlayService : Service() {

    // ── Constants ─────────────────────────────────────────────────────────────
    companion object {
        const val ACTION_ADD_TASK  = "com.mirchevsky.lifearchitect2.OVERLAY_ADD_TASK"
        const val ACTION_ADD_EVENT = "com.mirchevsky.lifearchitect2.OVERLAY_ADD_EVENT"
        const val ACTION_MIC       = "com.mirchevsky.lifearchitect2.OVERLAY_MIC"

        private const val CHANNEL_ID = "widget_overlay_channel"
        private const val NOTIF_ID   = 9001

        fun buildIntent(context: Context, action: String, widgetId: Int): Intent =
            Intent(context, WidgetOverlayService::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Lifecycle owner shim (required for ComposeView outside an Activity) ───
    private val lifecycleOwner = object : LifecycleOwner, SavedStateRegistryOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        val savedStateRegistryController = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.savedStateRegistryController.performRestore(null)
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires startForeground() to be called within 5 seconds of
        // startForegroundService() — call it FIRST before any other logic to
        // avoid ForegroundServiceDidNotStartInTimeException.
        startForegroundCompat()
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // Guard: if the overlay permission has not been granted yet, show a
        // friendly in-app dialog (via OverlayPermissionDialogActivity) that
        // explains the permission in plain language before deep-linking the user
        // to the app's own overlay toggle in Settings.
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(this, OverlayPermissionDialogActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        when (intent?.action) {
            ACTION_ADD_TASK  -> showOverlay(OverlayMode.ADD_TASK, widgetId)
            ACTION_MIC       -> showOverlay(OverlayMode.MIC, widgetId)
            ACTION_ADD_EVENT -> showOverlay(OverlayMode.ADD_EVENT, widgetId)
            else             -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Foreground notification ───────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Life Architect Widget",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Used while the task input panel is open"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun startForegroundCompat() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.widget_ic_add)
            .setContentTitle("Life Architect")
            .setContentText("Adding task…")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    // ── Overlay management ────────────────────────────────────────────────────
    private fun showOverlay(mode: OverlayMode, widgetId: Int) {
        removeOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OverlayPanel(
                    mode = mode,
                    onDismiss = { dismissOverlay(widgetId, taskSaved = false) },
                    onSaved   = { dismissOverlay(widgetId, taskSaved = true) }
                )
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    /**
     * Dismisses the overlay and, when a task was actually saved, notifies the
     * widget's RemoteViewsFactory to reload its data from Room.
     *
     * notifyAppWidgetViewDataChanged() is the correct and complete mechanism for
     * refreshing a collection widget's data. Calling setRemoteAdapter() again
     * (via updateAppWidget) after this call would reset the adapter to "Loading…"
     * and race with the factory's onDataSetChanged(), so we intentionally do NOT
     * send a WIDGET_REFRESH broadcast here.
     */
    private fun dismissOverlay(widgetId: Int, taskSaved: Boolean) {
        removeOverlay()

        if (taskSaved) {
            serviceScope.launch {
                val manager = AppWidgetManager.getInstance(this@WidgetOverlayService)
                val ids = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    intArrayOf(widgetId)
                } else {
                    manager.getAppWidgetIds(
                        ComponentName(this@WidgetOverlayService, TaskWidgetProvider::class.java)
                    )
                }

                // Tell the RemoteViewsFactory to call onDataSetChanged() and reload
                // from Room. This is the only call needed — do not call updateAppWidget
                // or sendRefreshBroadcast after this.
                @Suppress("DEPRECATION")
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)

            }.invokeOnCompletion { stopSelf() }
        } else {
            stopSelf()
        }
    }

    // ── Compose UI ────────────────────────────────────────────────────────────
    @Composable
    private fun OverlayPanel(
        mode: OverlayMode,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (visible) 0.55f else 0f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(320)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(260)
                )
            ) {
                Box(modifier = Modifier.clickable(enabled = false, onClick = {})) {
                    when (mode) {
                        OverlayMode.ADD_TASK  -> AddTaskPanel(onDismiss, onSaved)
                        OverlayMode.MIC       -> MicPanel(onDismiss, onSaved)
                        OverlayMode.ADD_EVENT -> AddEventPanel(onDismiss, onSaved)
                    }
                }
            }
        }
    }

    // ── Add Task panel ────────────────────────────────────────────────────────
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddTaskPanel(onDismiss: () -> Unit, onSaved: () -> Unit) {
        var title      by remember { mutableStateOf("") }
        var difficulty by remember { mutableStateOf("medium") }
        var isSaving   by remember { mutableStateOf(false) }
        val keyboard   = LocalSoftwareKeyboardController.current

        fun save() {
            if (title.isBlank()) return
            isSaving = true
            serviceScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).taskDao().upsertTask(
                    TaskEntity(
                        id          = UUID.randomUUID().toString(),
                        userId      = "local_user",
                        title       = title.trim(),
                        difficulty  = difficulty,
                        status      = "pending",
                        isCompleted = false
                    )
                )
                launch(Dispatchers.Main) {
                    keyboard?.hide()
                    onSaved()
                }
            }
        }

        PanelSurface {
            DragHandle()
            PanelTitle("New Task", onDismiss)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = outlinedTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))
            Text("Difficulty", fontSize = 12.sp, color = Color(0xFF9999BB))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DifficultyChip("Easy",   "easy",   Color(0xFF22C55E), difficulty) { difficulty = it }
                DifficultyChip("Medium", "medium", Color(0xFFF59E0B), difficulty) { difficulty = it }
                DifficultyChip("Hard",   "hard",   Color(0xFFEF4444), difficulty) { difficulty = it }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Text(if (isSaving) "Saving…" else "Save Task",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // ── Mic / Voice panel ─────────────────────────────────────────────────────
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MicPanel(onDismiss: () -> Unit, onSaved: () -> Unit) {
        // ── Audio permission guard ────────────────────────────────────────────
        val hasAudio = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudio) {
            startActivity(
                Intent(applicationContext, MicPermissionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            onDismiss()
            return
        }
        // ─────────────────────────────────────────────────────────────────────
        var title       by remember { mutableStateOf("") }
        var difficulty  by remember { mutableStateOf("medium") }
        var isListening by remember { mutableStateOf(true) }
        var statusText  by remember { mutableStateOf("Listening…") }
        var isSaving    by remember { mutableStateOf(false) }
        val keyboard    = LocalSoftwareKeyboardController.current

        DisposableEffect(Unit) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { statusText = "Listening…" }
                override fun onBeginningOfSpeech() { statusText = "Hearing you…" }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { statusText = "Processing…" }
                override fun onError(error: Int) { isListening = false; statusText = "Tap the field to type instead" }
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) title = matches[0]
                    isListening = false; statusText = "Edit or save"
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!partial.isNullOrEmpty()) title = partial[0]
                }
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            recognizer.startListening(recognizerIntent)
            onDispose { recognizer.destroy() }
        }

        fun save() {
            if (title.isBlank()) return
            isSaving = true
            serviceScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).taskDao().upsertTask(
                    TaskEntity(
                        id          = UUID.randomUUID().toString(),
                        userId      = "local_user",
                        title       = title.trim(),
                        difficulty  = difficulty,
                        status      = "pending",
                        isCompleted = false
                    )
                )
                launch(Dispatchers.Main) { keyboard?.hide(); onSaved() }
            }
        }

        PanelSurface {
            DragHandle()
            PanelTitle("Voice Input", onDismiss)
            Spacer(Modifier.height(12.dp))
            Text(statusText, fontSize = 13.sp, color = Color(0xFF10B981))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; isListening = false },
                label = { Text("Transcribed text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = outlinedTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))
            Text("Difficulty", fontSize = 12.sp, color = Color(0xFF9999BB))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DifficultyChip("Easy",   "easy",   Color(0xFF22C55E), difficulty) { difficulty = it }
                DifficultyChip("Medium", "medium", Color(0xFFF59E0B), difficulty) { difficulty = it }
                DifficultyChip("Hard",   "hard",   Color(0xFFEF4444), difficulty) { difficulty = it }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Text(if (isSaving) "Saving…" else "Save Task",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // ── Add Calendar Event panel ──────────────────────────────────────────────
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddEventPanel(onDismiss: () -> Unit, onSaved: () -> Unit) {
        var eventTitle by remember { mutableStateOf("") }
        var isSaving   by remember { mutableStateOf(false) }
        val keyboard   = LocalSoftwareKeyboardController.current

        fun save() {
            if (eventTitle.isBlank()) return
            isSaving = true
            serviceScope.launch(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis() + 3_600_000L
                val endMillis   = startMillis + 3_600_000L
                val values = android.content.ContentValues().apply {
                    put(android.provider.CalendarContract.Events.TITLE, eventTitle.trim())
                    put(android.provider.CalendarContract.Events.DTSTART, startMillis)
                    put(android.provider.CalendarContract.Events.DTEND, endMillis)
                    put(android.provider.CalendarContract.Events.EVENT_TIMEZONE,
                        java.util.TimeZone.getDefault().id)
                    put(android.provider.CalendarContract.Events.CALENDAR_ID, 1L)
                }
                try {
                    contentResolver.insert(
                        android.provider.CalendarContract.Events.CONTENT_URI, values)
                } catch (_: Exception) {}
                launch(Dispatchers.Main) { keyboard?.hide(); onSaved() }
            }
        }

        PanelSurface {
            DragHandle()
            PanelTitle("New Calendar Event", onDismiss)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Event title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = outlinedTextFieldColors()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = eventTitle.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(if (isSaving) "Creating…" else "Create Event",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // ── Shared Compose components ─────────────────────────────────────────────
    @Composable
    private fun PanelSurface(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF1C1E2E))
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            content = content
        )
    }

    @Composable
    private fun DragHandle() {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF4A4D6A))
            )
        }
    }

    @Composable
    private fun PanelTitle(title: String, onDismiss: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFEEEEFF))
            TextButton(onClick = onDismiss) {
                Text("✕", fontSize = 16.sp, color = Color(0xFF9999BB))
            }
        }
    }

    @Composable
    private fun DifficultyChip(
        label: String,
        value: String,
        activeColor: Color,
        selected: String,
        onSelect: (String) -> Unit
    ) {
        val isSelected = selected == value
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) activeColor else Color(0xFF2A2D40))
                .clickable { onSelect(value) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF9999BB))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Color(0xFF10B981),
        unfocusedBorderColor = Color(0xFF3A3D55),
        focusedLabelColor    = Color(0xFF10B981),
        unfocusedLabelColor  = Color(0xFF9999BB),
        cursorColor          = Color(0xFF10B981),
        focusedTextColor     = Color(0xFFEEEEFF),
        unfocusedTextColor   = Color(0xFFEEEEFF)
    )
}

// ── Mode enum ─────────────────────────────────────────────────────────────────
enum class OverlayMode { ADD_TASK, MIC, ADD_EVENT }
