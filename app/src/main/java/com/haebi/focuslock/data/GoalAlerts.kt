package com.haebi.focuslock.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.haebi.focuslock.MainActivity
import com.haebi.focuslock.ui.hasNotificationAccess
import com.haebi.focuslock.ui.hasUsageAccess
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** If an app crosses both limits between checks, only the higher warning is sent. */
object GoalAlertPolicy {
    fun threshold(minutes: Int, goal: Int, warn80: Boolean, warn100: Boolean): Int? {
        if (goal <= 0 || minutes <= 0) return null
        return when {
            warn100 && minutes >= goal -> 100
            warn80 && minutes.toLong() * 100 >= goal.toLong() * 80 -> 80
            else -> null
        }
    }
}

/** No accessibility service, overlay, app blocking, or network collection. */
object GoalAlerts {
    private const val PREFS = "focuslock_ui_prototype"
    private const val CHANNEL = "focuslock_goal_warning"
    private const val UNIQUE_WORK = "focuslock_goal_periodic"
    private val notificationLock = Any()

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val hasGoals = prefs.all.any { (key, value) -> key.startsWith("goal.") && value is Int && value > 0 }
        val work = WorkManager.getInstance(context.applicationContext)
        if (hasGoals) {
            val request = PeriodicWorkRequestBuilder<GoalAlertWorker>(15, TimeUnit.MINUTES).build()
            work.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(UNIQUE_WORK)
        }
    }

    fun checkNow(context: Context) {
        if (!hasUsageAccess(context) || !hasNotificationAccess(context)) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.areNotificationsEnabled()) return
        val state = UsageRepository(context.applicationContext).load()
        val snapshot = (state as? UsageLoadState.Ready)?.snapshot ?: return
        val prefs = context.getSharedPreferences(PREFS, 0)
        val today = LocalDate.now().toEpochDay()
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL, "사용 목표 알림", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        snapshot.today.apps.forEach { app ->
            val goal = prefs.getInt("goal.${app.id}", 0)
            if (goal <= 0 || !prefs.getBoolean("everyday.${app.id}", true)) return@forEach
            val threshold = GoalAlertPolicy.threshold(
                minutes = app.minutes,
                goal = goal,
                warn80 = prefs.getBoolean("warn80.${app.id}", true),
                warn100 = prefs.getBoolean("warn100.${app.id}", true)
            ) ?: return@forEach
            val key = "goal_alert.${today}.${app.id}.${threshold}"
            synchronized(notificationLock) {
                if (prefs.getBoolean(key, false)) return@synchronized
                val title = if (threshold == 100) "사용 목표에 도달했습니다" else "사용 목표의 80%에 도달했습니다"
                val notification = Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText("${app.name} · 오늘 ${app.minutes}분 / 목표 ${goal}분")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                try {
                    notificationManager.notify((today.toString() + app.id + threshold).hashCode(), notification)
                    val editor = prefs.edit().putBoolean(key, true)
                    if (threshold == 100) editor.putBoolean("goal_alert.${today}.${app.id}.80", true)
                    editor.commit()
                } catch (_: SecurityException) {
                    // Do not mark sent when the user has revoked notification access.
                }
            }
        }
        // Prevent a growing preference file from accumulating historical delivery markers.
        val stale = prefs.all.keys.filter { key ->
            if (!key.startsWith("goal_alert.")) return@filter false
            val day = key.removePrefix("goal_alert.").substringBefore('.').toLongOrNull()
            day != null && day < today - 45
        }
        if (stale.isNotEmpty()) prefs.edit().also { editor -> stale.forEach(editor::remove) }.apply()
    }
}

class GoalAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GoalAlerts.checkNow(applicationContext)
        }
        Result.success()
    } catch (_: RuntimeException) {
        Result.retry()
    }
}
