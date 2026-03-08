package com.mirchevsky.lifearchitect2.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.mirchevsky.lifearchitect2.R
import com.mirchevsky.lifearchitect2.data.db.AppDatabase
import com.mirchevsky.lifearchitect2.data.db.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * WidgetVoiceActivity
 * ─────────────────────────────────────────────────────────────────────────────
 * A lightweight, dialog-themed [Activity] launched when the user taps the mic
 * button on the task list widget.
 *
 * Behaviour:
 *   1. Opens as a floating dialog over the home screen.
 *   2. Immediately starts Android's [SpeechRecognizer] — no system overlay,
 *      no privacy notice dialog. Recognises in the device's default language,
 *      matching the behaviour of AddTaskItem.kt.
 *   3. Displays a status label ("Listening…" / "Tap mic to retry") and an
 *      [EditText] pre-filled with the recognised text.
 *   4. The user can edit the text and tap Save, or tap the mic button again
 *      to re-record, or tap Cancel to dismiss.
 *   5. On Save: inserts a [TaskEntity] into Room and sends
 *      [TaskWidgetProvider.ACTION_WIDGET_REFRESH] so the widget list updates
 *      immediately.
 *
 * Requires: android.permission.RECORD_AUDIO (declared in AndroidManifest).
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/WidgetVoiceActivity.kt
 */
class WidgetVoiceActivity : Activity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var statusLabel: TextView
    private lateinit var inputField: EditText
    private lateinit var micButton: ImageButton
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_voice)

        // Dialog window styling — dark card with rounded corners, dimmed background
        window.setBackgroundDrawableResource(R.drawable.widget_background)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.also { it.dimAmount = 0.6f }

        statusLabel  = findViewById(R.id.widget_voice_status)
        inputField   = findViewById(R.id.widget_voice_input)
        micButton    = findViewById(R.id.widget_voice_mic_btn)
        cancelButton = findViewById(R.id.widget_voice_cancel)
        saveButton   = findViewById(R.id.widget_voice_save)

        cancelButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val title = inputField.text.toString().trim()
            if (title.isBlank()) {
                inputField.error = "Please enter a task title"
                return@setOnClickListener
            }
            saveTask(title)
        }

        micButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }

        // Start listening immediately on open
        startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // ── Speech recognition ────────────────────────────────────────────────────

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusLabel.text = "Listening…"
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                statusLabel.text = "Processing…"
            }

            override fun onError(error: Int) {
                isListening = false
                statusLabel.text = "Tap mic to retry"
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    inputField.setText(text)
                    inputField.setSelection(text.length)
                    statusLabel.text = "Edit if needed, then Save"
                } else {
                    statusLabel.text = "Nothing heard — tap mic to retry"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        statusLabel.text = "Tap mic to retry"
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveTask(title: String) {
        val task = TaskEntity(
            title       = title,
            difficulty  = "medium",
            userId      = "local_user",
            status      = "pending",
            isCompleted = false
        )

        activityScope.launch {
            AppDatabase.getDatabase(applicationContext)
                .taskDao()
                .upsertTask(task)

            runOnUiThread {
                // Notify the widget's RemoteViewsFactory to reload from Room.
                // We use notifyAppWidgetViewDataChanged directly — do NOT call
                // sendRefreshBroadcast / updateAppWidget here, as re-binding the
                // RemoteAdapter after a data-change notification causes the
                // collection to get stuck on "Loading…".
                val manager = AppWidgetManager.getInstance(applicationContext)
                val ids = manager.getAppWidgetIds(
                    ComponentName(applicationContext, TaskWidgetProvider::class.java)
                )
                @Suppress("DEPRECATION")
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)

                Toast.makeText(this@WidgetVoiceActivity, "Task added", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
