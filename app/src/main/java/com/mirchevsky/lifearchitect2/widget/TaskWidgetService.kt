package com.mirchevsky.lifearchitect2.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * TaskWidgetService
 * ─────────────────────────────────────────────────────────────────────────────
 * A [RemoteViewsService] that supplies the [TaskWidgetItemFactory] to the
 * widget's [android.widget.ListView].
 *
 * The home screen process binds to this service when it needs to populate the
 * task list. This class is intentionally minimal — all data logic lives in
 * [TaskWidgetItemFactory].
 *
 * Registered in AndroidManifest.xml with:
 *   - android:permission="android.permission.BIND_REMOTEVIEWS"
 *   - intent-filter action "android.appwidget.action.APPWIDGET_UPDATE"
 *
 * Place at:
 *   app/src/main/java/com/mirchevsky/lifearchitect2/widget/TaskWidgetService.kt
 */
class TaskWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetItemFactory(applicationContext, intent)
    }
}
