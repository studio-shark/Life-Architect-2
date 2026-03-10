package com.mirchevsky.lifearchitect2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.mirchevsky.lifearchitect2.R
import com.mirchevsky.lifearchitect2.data.db.AppDatabase
import com.mirchevsky.lifearchitect2.data.db.entity.TaskEntity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * TaskWidgetProvider
 * ─────────────────────────────────────────────────────────────────────────────
 * AppWidgetProvider for the Life Architect task list widget.
 *
 * Uses the modern [RemoteViews.RemoteCollectionItems] API (API 31+) instead of
 * the deprecated [android.widget.RemoteViewsService] pattern.
 *
 * ## Interactive row actions
 *
 * Each row exposes three tap targets:
 *   - [R.id.widget_item_dot]  → complete the task
 *   - [R.id.widget_item_flag] → toggle urgent (isUrgent)
 *   - [R.id.widget_item_pin]  → toggle pinned (isPinned)
 *
 * Because [RemoteViews.RemoteCollectionItems] only supports a single
 * [RemoteViews.setPendingIntentTemplate] per list, all three actions share one
 * template intent ([ACTION_WIDGET_ROW_ACTION]). Each fill-in intent carries:
 *   - [EXTRA_TASK_ID]  — the task's string ID
 *   - [EXTRA_ROW_ACTION] — one of [ROW_ACTION_COMPLETE], [ROW_ACTION_TOGGLE_FLAG],
 *                          or [ROW_ACTION_TOGGLE_PIN]
 *
 * [onReceive] dispatches on [EXTRA_ROW_ACTION] and performs the appropriate
 * Room mutation on a background thread before pushing a fresh widget.
 *
 * ## Checkbox tint
 *
 * The checkbox ([R.id.widget_item_dot]) is NOT tinted by task state. Only the
 * title text colour and the flag/pin icon tints change for urgent/pinned tasks.
 * This prevents the coloured-box visual artefact seen when setColorFilter was
 * applied to the checkbox ImageView.
 *
 * ## Threading
 *
 * All Room access uses [runBlocking] on a background [Thread] started after
 * [goAsync]. This guarantees the full task list is available before
 * [AppWidgetManager.updateAppWidget] is called, preventing blank rows.
 */
class TaskWidgetProvider : AppWidgetProvider() {

    companion object {
        // ── Broadcast actions ─────────────────────────────────────────────────
        const val ACTION_WIDGET_REFRESH  = "com.mirchevsky.lifearchitect2.WIDGET_REFRESH"
        const val ACTION_WIDGET_ROW_ACTION = "com.mirchevsky.lifearchitect2.WIDGET_ROW_ACTION"

        // ── Intent extras ─────────────────────────────────────────────────────
        const val EXTRA_TASK_ID    = "task_id"
        const val EXTRA_ROW_ACTION = "row_action"

        // ── Row action discriminators ─────────────────────────────────────────
        const val ROW_ACTION_COMPLETE     = "complete"
        const val ROW_ACTION_TOGGLE_FLAG  = "toggle_flag"
        const val ROW_ACTION_TOGGLE_PIN   = "toggle_pin"

        // ── Colours ───────────────────────────────────────────────────────────
        private val COLOR_URGENT  = Color.parseColor("#E53935")   // red
        private val COLOR_PINNED  = Color.parseColor("#F59E0B")   // amber
        // COLOR_ICON_INACTIVE is resolved at runtime via context.getColor(R.color.widget_icon_inactive)
        // so it is theme-aware (dark grey on light backgrounds, muted white on dark backgrounds).
        // It is NOT a compile-time constant — see buildTaskRow() for usage.

        private val DUE_DATE_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, HH:mm")

        /** Returns true if the SYSTEM_ALERT_WINDOW permission has been granted. */
        fun hasOverlayPermission(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        /**
         * Sends [ACTION_WIDGET_REFRESH] to all active instances of this provider.
         * Safe to call from any context (service, activity, ViewModel).
         */
        fun sendRefreshBroadcast(context: Context) {
            val intent = Intent(ACTION_WIDGET_REFRESH)
                .setClass(context, TaskWidgetProvider::class.java)
            context.sendBroadcast(intent)
        }
    }

    // ── AppWidgetProvider callbacks ───────────────────────────────────────────

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { id -> pushWidgetAsync(context, manager, id) }
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
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, TaskWidgetProvider::class.java)
                )
                ids.forEach { id -> pushWidgetAsync(context, manager, id) }
            }

            ACTION_WIDGET_ROW_ACTION -> {
                val taskId    = intent.getStringExtra(EXTRA_TASK_ID)    ?: return
                val rowAction = intent.getStringExtra(EXTRA_ROW_ACTION) ?: return

                val pendingResult = goAsync()
                Thread {
                    try {
                        val dao = AppDatabase.getDatabase(context).taskDao()

                        val task: TaskEntity? = runBlocking {
                            dao.getPendingTasksForUser("local_user")
                        }.firstOrNull { it.id == taskId }

                        var xpGained = 0

                        if (task != null) {
                            val updated = when (rowAction) {
                                ROW_ACTION_COMPLETE -> {
                                    xpGained = when (task.difficulty.lowercase()) {
                                        "easy"   -> 10
                                        "medium" -> 20
                                        "hard"   -> 35
                                        else     -> 15
                                    }
                                    task.copy(
                                        isCompleted = true,
                                        status      = "completed",
                                        completedAt = System.currentTimeMillis()
                                    )
                                }
                                ROW_ACTION_TOGGLE_FLAG -> task.copy(
                                    isUrgent = !task.isUrgent
                                )
                                ROW_ACTION_TOGGLE_PIN -> task.copy(
                                    isPinned = !task.isPinned
                                )
                                else -> null
                            }
                            if (updated != null) {
                                runBlocking { dao.upsertTask(updated) }
                            }
                        }

                        // Push fresh widget immediately after the DB write
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, TaskWidgetProvider::class.java)
                        )
                        ids.forEach { id -> pushWidget(context, manager, id) }

                        // Show XP popup overlay if a task was completed
                        if (xpGained > 0) {
                            context.startService(
                                Intent(context, WidgetOverlayService::class.java).apply {
                                    action = WidgetOverlayService.ACTION_SHOW_XP
                                    putExtra(WidgetOverlayService.EXTRA_XP_AMOUNT, xpGained)
                                    putExtra(WidgetOverlayService.EXTRA_XP_LABEL, "+$xpGained XP")
                                }
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }

    // ── Widget construction ───────────────────────────────────────────────────

    /**
     * Dispatches [pushWidget] to a background thread via [goAsync], extending
     * the BroadcastReceiver deadline so Room can be queried without blocking
     * the main thread or risking an ANR.
     */
    private fun pushWidgetAsync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val pendingResult = goAsync()
        Thread {
            try {
                pushWidget(context, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    /**
     * Queries Room synchronously (via [runBlocking]) for the current pending
     * task list, builds a full [RemoteViews] including a
     * [RemoteViews.RemoteCollectionItems] for the task list, and pushes it to
     * the home screen via [AppWidgetManager.updateAppWidget].
     *
     * Must be called from a background thread.
     */
    private fun pushWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 1. Load tasks from Room synchronously (called on a background thread)
        val tasks: List<TaskEntity> = runBlocking {
            AppDatabase.getDatabase(context)
                .taskDao()
                .getPendingTasksForUser("local_user")
        }

        // 2. Build the RemoteCollectionItems from the task list
        val items = buildCollectionItems(context, tasks)

        // 3. Build the outer widget RemoteViews
        val rv = RemoteViews(context.packageName, R.layout.widget_task_list)

        // ── 3a. Bind the collection using the modern non-deprecated API ──────
        rv.setRemoteAdapter(R.id.widget_task_list, items)
        rv.setEmptyView(R.id.widget_task_list, R.id.widget_empty_text)

        // ── 3b. Row action template intent ────────────────────────────────────
        // All three per-row tap targets (checkbox, flag, pin) share this single
        // template. The fill-in intent on each view adds EXTRA_TASK_ID and
        // EXTRA_ROW_ACTION to discriminate between actions.
        val rowActionTemplate = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(ACTION_WIDGET_ROW_ACTION)
                .setClass(context, TaskWidgetProvider::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        rv.setPendingIntentTemplate(R.id.widget_task_list, rowActionTemplate)

        // ── 3c. Mic button ────────────────────────────────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_mic,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_MIC, appWidgetId, 10)
        )

        // ── 3d. Calendar button ───────────────────────────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_add_event,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_ADD_EVENT, appWidgetId, 20)
        )

        // ── 3e. Add Task button ───────────────────────────────────────────────
        rv.setOnClickPendingIntent(
            R.id.widget_btn_add_task,
            overlayServiceIntent(context, WidgetOverlayService.ACTION_ADD_TASK, appWidgetId, 30)
        )

        // 4. Push the complete RemoteViews to the home screen
        appWidgetManager.updateAppWidget(appWidgetId, rv)
    }

    // ── Collection building ───────────────────────────────────────────────────

    private fun buildCollectionItems(
        context: Context,
        tasks: List<TaskEntity>
    ): RemoteViews.RemoteCollectionItems {
        // setViewTypeCount(3) with a 3-argument addItem() overload requires API 35.
        // For pre-API-35 compatibility we instead use 3 separate layout resource IDs
        // (widget_task_item, widget_task_item_urgent, widget_task_item_pinned).
        // Because each layout has a different resource ID, the launcher's
        // RemoteCollectionItems recycler treats them as different view types and
        // maintains separate view caches — a pinned-row view is never handed to a
        // normal or urgent row, so setTextColor is always applied to a fresh view.
        val builder = RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(3)

        tasks.forEach { task ->
            val itemRv = buildTaskRow(context, task)
            val stableId = task.id.hashCode().toLong()
            builder.addItem(stableId, itemRv)
        }

        return builder.build()
    }

    /**
     * Builds a single row [RemoteViews] for [task].
     *
     * Visual rules (mirror of in-app TaskItem):
     *   - Title colour priority: urgent red > pinned amber > XML default
     *   - Flag icon:  red if urgent, muted grey otherwise (independent of pin)
     *   - Pin icon:   amber if pinned, muted grey otherwise (independent of flag)
     *   - Both states can be active simultaneously — the icons are independent.
     *
     * Layout selection uses 4 distinct resource IDs so the launcher's
     * RemoteCollectionItems recycler maintains a separate view cache per state
     * combination. This prevents a tinted view from being recycled into a row
     * with a different state.
     *
     * The checkbox ([R.id.widget_item_dot]) is NEVER tinted — it always renders
     * with its XML default appearance to avoid the coloured-box artefact.
     */
    private fun buildTaskRow(context: Context, task: TaskEntity): RemoteViews {
        // 4 layout variants — one per state combination — so the launcher's
        // view recycler never hands a tinted view to a different-state row.
        val layoutRes = when {
            task.isUrgent && task.isPinned -> R.layout.widget_task_item_urgent_pinned
            task.isUrgent                  -> R.layout.widget_task_item_urgent
            task.isPinned                  -> R.layout.widget_task_item_pinned
            else                           -> R.layout.widget_task_item
        }
        val rv = RemoteViews(context.packageName, layoutRes)

        // Resolve theme-aware inactive icon tint at runtime.
        // Light mode: dark at 40% opacity (#661C1C2E, matches checkbox stroke).
        // Dark mode:  white at 40% opacity (#66FFFFFF).
        val colorIconInactive = context.getColor(R.color.widget_icon_inactive)

        // ── Title ─────────────────────────────────────────────────────────────
        rv.setTextViewText(R.id.widget_item_title, task.title)

        // ── Title colour (priority: urgent > pinned > XML default) ────────────
        //
        // Normal tasks: do NOT call setTextColor — let the XML-defined
        // @color/widget_text_primary apply (theme-aware, visible on both light
        // and dark widget backgrounds).
        when {
            task.isUrgent -> rv.setTextColor(R.id.widget_item_title, COLOR_URGENT)
            task.isPinned -> rv.setTextColor(R.id.widget_item_title, COLOR_PINNED)
            // else: no setTextColor call — XML default applies
        }

        // ── Icon tints — INDEPENDENT of each other ────────────────────────────
        //
        // Flag and pin states are orthogonal. A task can be both urgent and
        // pinned simultaneously. Each icon reflects only its own state.
        val flagTint = if (task.isUrgent) COLOR_URGENT  else colorIconInactive
        val pinTint  = if (task.isPinned) COLOR_PINNED  else colorIconInactive
        rv.setInt(R.id.widget_item_flag, "setColorFilter", flagTint)
        rv.setInt(R.id.widget_item_pin,  "setColorFilter", pinTint)

        // ── Checkbox: NO tint change — always renders with XML default ────────
        // (Removing the setColorFilter call on widget_item_dot fixes the
        //  coloured-box artefact reported by the user.)

        // ── Calendar event indicator ───────────────────────────────────────────
        // Flag and pin are shown on ALL rows (tasks and events alike).
        // The calendar icon is shown ONLY on event rows, alongside the icons.
        val isEvent = task.status == "event"
        rv.setViewVisibility(
            R.id.widget_item_calendar,
            if (isEvent) View.VISIBLE else View.GONE
        )
        if (isEvent) {
            rv.setInt(R.id.widget_item_calendar, "setColorFilter", colorIconInactive)
        }

        // ── Due date / event date+time ──────────────────────────────────────
        // All-day events are stored at UTC midnight (time-of-day == 00:00 UTC).
        // Timed events are stored at the actual local time converted to epoch millis.
        // We detect all-day by checking if the UTC time-of-day is exactly midnight.
        val dueDate = task.dueDate
        if (dueDate != null) {
            val zone       = ZoneId.systemDefault()
            val dueInstant = Instant.ofEpochMilli(dueDate)
            val dueUtc     = dueInstant.atZone(ZoneOffset.UTC)
            val isAllDay   = dueUtc.hour == 0 && dueUtc.minute == 0 && dueUtc.second == 0
            val dueZdt     = dueInstant.atZone(zone)
            val dueLocal   = dueZdt.toLocalDate()
            val today      = java.time.LocalDate.now(zone)
            val label = if (isAllDay) {
                // All-day: show date only, no time
                when (dueLocal) {
                    today              -> "Today"
                    today.plusDays(1)  -> "Tomorrow"
                    today.minusDays(1) -> "Yesterday"
                    else               -> dueZdt.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    )
                }
            } else {
                // Timed: show date + time
                val timePart = dueZdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                when (dueLocal) {
                    today              -> "Today, $timePart"
                    today.plusDays(1)  -> "Tomorrow, $timePart"
                    today.minusDays(1) -> "Yesterday, $timePart"
                    else               -> dueZdt.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")
                    )
                }
            }
            rv.setTextViewText(R.id.widget_item_due, label)
            rv.setViewVisibility(R.id.widget_item_date_row, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.widget_item_date_row, View.GONE)
        }

        // ── Fill-in intents for each tap target ───────────────────────────────
        //
        // Each fill-in intent is merged with the template PendingIntent set on
        // the list in pushWidget(). The template provides the action and class;
        // the fill-in adds the task ID and the specific row action.

        // Checkbox → complete task
        rv.setOnClickFillInIntent(
            R.id.widget_item_dot,
            Intent().apply {
                putExtra(EXTRA_TASK_ID,    task.id)
                putExtra(EXTRA_ROW_ACTION, ROW_ACTION_COMPLETE)
            }
        )

        // Flag icon → toggle urgent
        rv.setOnClickFillInIntent(
            R.id.widget_item_flag,
            Intent().apply {
                putExtra(EXTRA_TASK_ID,    task.id)
                putExtra(EXTRA_ROW_ACTION, ROW_ACTION_TOGGLE_FLAG)
            }
        )

        // Pin icon → toggle pinned
        rv.setOnClickFillInIntent(
            R.id.widget_item_pin,
            Intent().apply {
                putExtra(EXTRA_TASK_ID,    task.id)
                putExtra(EXTRA_ROW_ACTION, ROW_ACTION_TOGGLE_PIN)
            }
        )

        return rv
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
