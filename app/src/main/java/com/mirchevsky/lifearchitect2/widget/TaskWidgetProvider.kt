package com.mirchevsky.lifearchitect2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.RemoteViews
import com.mirchevsky.lifearchitect2.R
import com.mirchevsky.lifearchitect2.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * TaskWidgetProvider
 * ─────────────────────────────────────────────────────────────────────────────
 * AppWidgetProvider for the Life Architect task list widget.
 *
 * Button wiring:
 *   + (Add Task)  → starts WidgetOverlayService with ACTION_ADD_TASK
 *   📅 (Calendar) → starts WidgetOverlayService with ACTION_ADD_EVENT
 *   🎤 (Mic)      → starts WidgetOverlayService with ACTION_MIC
 *
 * Task completion:
 *   Each row has a fill-in intent carrying the task ID.
 *   The provider intercepts ACTION_COMPLETE_TASK, marks the task done in Room,
 *   and calls notifyAppWidgetViewDataChanged() to refresh the list.
 *
 * Refresh strategy:
 *   setRemoteAdapter() is called ONLY during initial/full widget setup (onUpdate,
 *   onEnabled). For data-only refreshes (task add, task complete) we call ONLY
 *   notifyAppWidgetViewDataChanged(). Re-binding the adapter after a data-change
 *   notification resets the collection to "Loading…" and races with the factory's
 *   onDataSetChanged(), so we deliberately avoid it on the refresh path.
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/TaskWidgetProvider.kt
 */
class TaskWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.mirchevsky.lifearchitect2.WIDGET_REFRESH"
        const val ACTION_COMPLETE_TASK  = "com.mirchevsky.lifearchitect2.COMPLETE_TASK"
        const val EXTRA_TASK_ID         = "task_id"

        /** Returns true if the SYSTEM_ALERT_WINDOW permission has been granted. */
        fun hasOverlayPermission(context: Context): Boolean =
            Settings.canDrawOverlays(context)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── AppWidgetProvider callbacks ───────────────────────────────────────────

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        // Full setup including adapter binding
        ids.forEach { id -> bindWidget(context, manager, id, bindCollection = true) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, TaskWidgetProvider::class.java)
        )
        onUpdate(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                // This broadcast is kept for compatibility (e.g. called from the
                // main app after a task mutation). We update chrome only — no
                // setRemoteAdapter() — to avoid resetting the collection.
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, TaskWidgetProvider::class.java)
                )
                ids.forEach { id -> bindWidget(context, manager, id, bindCollection = false) }
            }

            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val task = db.taskDao()
                        .observeTasksForUser("local_user")
                        .first()
                        .firstOrNull { it.id == taskId } ?: return@launch
                    db.taskDao().upsertTask(
                        task.copy(
                            isCompleted = true,
                            status = "completed",
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, TaskWidgetProvider::class.java)
                )
                @Suppress("DEPRECATION")
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)
            }
        }
    }

    // ── Widget setup ──────────────────────────────────────────────────────────

    /**
     * Builds and pushes a RemoteViews update for [appWidgetId].
     *
     * @param bindCollection When true, calls setRemoteAdapter() to perform the
     *   initial (or full) adapter binding. Pass false for chrome-only updates
     *   (button intents, pending intent template) that must not disturb an
     *   already-running data refresh cycle.
     */
    private fun bindWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        bindCollection: Boolean
    ) {
        val rv = RemoteViews(context.packageName, R.layout.widget_task_list)

        // ── 1. Remote adapter (initial bind only) ─────────────────────────────
        if (bindCollection) {
            val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            rv.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
            rv.setEmptyView(R.id.widget_task_list, R.id.widget_empty_text)
        }

        // ── 2. Task completion template intent ────────────────────────────────
        val completionTemplate = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(ACTION_COMPLETE_TASK).setClass(context, TaskWidgetProvider::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        rv.setPendingIntentTemplate(R.id.widget_task_list, completionTemplate)

        // ── 3. Mic button → WidgetOverlayService ──────────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_mic,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_MIC, appWidgetId, 10)
        )

        // ── 4. Calendar button → WidgetOverlayService ─────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_add_event,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_ADD_EVENT, appWidgetId, 20)
        )

        // ── 5. Add Task button → WidgetOverlayService ─────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_add_task,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_ADD_TASK, appWidgetId, 30)
        )

        // ── 6. Apply ──────────────────────────────────────────────────────────
        appWidgetManager.updateAppWidget(appWidgetId, rv)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun overlayServiceIntent(
        context: Context,
        action: String,
        widgetId: Int,
        requestOffset: Int
    ): PendingIntent {
        val intent = WidgetOverlayService.buildIntent(context, action, widgetId)
        return PendingIntent.getForegroundService(
            context,
            widgetId * 100 + requestOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
