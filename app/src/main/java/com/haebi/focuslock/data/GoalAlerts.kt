package com.haebi.focuslock.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.haebi.focuslock.ui.GoalNotificationAvailability
import com.haebi.focuslock.ui.goalNotificationAvailability
import com.haebi.focuslock.ui.hasUsageAccess
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** If both limits are crossed between checks, send only the higher warning. */
object GoalAlertPolicy {
    fun threshold(minutes: Int, goal: Int, warn80: Boolean, warn100: Boolean): Int? =
        thresholdMillis(minutes.toLong() * 60_000L, goal, warn80, warn100)

    fun thresholdMillis(millis: Long, goal: Int, warn80: Boolean, warn100: Boolean): Int? {
        if (goal <= 0 || millis <= 0) return null
        val limit = goal.toLong() * 60_000L
        return when {
            warn100 && millis >= limit -> 100
            warn80 && millis >= limit * 80 / 100 -> 80
            else -> null
        }
    }
}

object GoalAlerts {
    private const val PREFS = "focuslock_ui_prototype"
    internal const val CHANNEL = "focuslock_goal_warning"
    private const val UNIQUE_WORK = "focuslock_goal_periodic"
    private val notificationLock = Any()

    private fun daysForApp(prefs: android.content.SharedPreferences, appId: String): Int =
        if (prefs.contains("goalDays.$appId")) {
            GoalDayPolicy.normalize(prefs.getInt("goalDays.$appId", GoalDayPolicy.EVERY_DAY))
        } else GoalDayPolicy.fromLegacyEveryday(prefs.getBoolean("everyday.$appId", true))

    private fun enabledForApp(prefs: android.content.SharedPreferences, id: String): Boolean =
        prefs.getBoolean("warn80.$id", true) || prefs.getBoolean("warn100.$id", true)

    internal fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "사용 목표 알림", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val hasAppGoals = prefs.all.any { (key, value) ->
            key.startsWith("goal.") && value is Int && value > 0 &&
                daysForApp(prefs, key.removePrefix("goal.")) != GoalDayPolicy.NONE &&
                enabledForApp(prefs, key.removePrefix("goal."))
        }
        val hasTotalGoal = prefs.getInt(GoalHistoryStore.TOTAL_KEY, 0) > 0 &&
            prefs.getBoolean(GoalHistoryStore.TOTAL_ALERT_KEY, false)
        val work = WorkManager.getInstance(context.applicationContext)
        if (hasAppGoals || hasTotalGoal) {
            val request = PeriodicWorkRequestBuilder<GoalAlertWorker>(15, TimeUnit.MINUTES).build()
            work.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else work.cancelUniqueWork(UNIQUE_WORK)
    }

    fun checkNow(context: Context, snapshot: UsageSnapshot? = null) {
        if (!hasUsageAccess(context)) return
        val before = goalNotificationAvailability(context)
        if (before != GoalNotificationAvailability.READY && before != GoalNotificationAvailability.CHANNEL_MISSING) return
        if (before == GoalNotificationAvailability.CHANNEL_MISSING) ensureChannel(context)
        if (goalNotificationAvailability(context) != GoalNotificationAvailability.READY) return
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault().id
        val date = LocalDate.now()
        val recent = snapshot?.takeIf {
            it.today.endDate == date && it.observedZoneId == zone && now - it.collectedAtMillis in 0L..60_000L
        }?.let { GoalUsageReading(date, zone, it.today.totalForegroundMillis, it.today.apps) }
        val reading = recent ?: UsageRepository(context.applicationContext).loadTodayForAlerts(now) ?: return
        if (reading.date != LocalDate.now() || reading.zoneId != ZoneId.systemDefault().id) return
        val prefs = context.getSharedPreferences(PREFS, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        val today = reading.date.toEpochDay()

        fun send(scope: String, label: String, millis: Long, total: Boolean) {
            synchronized(notificationLock) {
                if (!hasUsageAccess(context) || goalNotificationAvailability(context) != GoalNotificationAvailability.READY ||
                    LocalDate.now() != reading.date || ZoneId.systemDefault().id != reading.zoneId) return@synchronized
                // Re-read the current goal settings after obtaining the lock.
                val limit = if (total) prefs.getInt(GoalHistoryStore.TOTAL_KEY, 0) else prefs.getInt("goal.$scope", 0)
                val enabled = if (total) prefs.getBoolean(GoalHistoryStore.TOTAL_ALERT_KEY, false)
                    else GoalDayPolicy.selected(daysForApp(prefs, scope), reading.date)
                if (!enabled) return@synchronized
                val threshold = GoalAlertPolicy.thresholdMillis(millis, limit,
                    total || prefs.getBoolean("warn80.$scope", true),
                    total || prefs.getBoolean("warn100.$scope", true)) ?: return@synchronized
                val key = "goal_alert.$today.$scope.$threshold"
                if (prefs.getBoolean(key, false)) return@synchronized
                val title = if (threshold == 100) "사용 목표에 도달했습니다" else "사용 목표의 80%에 도달했습니다"
                val notification = Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText("$label · 오늘 ${UsageMetrics.roundedMinutes(millis)}분 / 목표 ${limit}분")
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setPublicVersion(Notification.Builder(context, CHANNEL)
                        .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("FocusLock 목표 알림").build())
                    .setContentIntent(GoalAlertNavigation.pendingIntent(context, scope))
                    .setAutoCancel(true).build()
                try {
                    // This records a submission request, not guaranteed OS delivery.
                    manager.notify((today.toString() + scope + threshold).hashCode(), notification)
                    if (goalNotificationAvailability(context) != GoalNotificationAvailability.READY) return@synchronized
                    val editor = prefs.edit().putBoolean(key, true)
                    if (threshold == 100) editor.putBoolean("goal_alert.$today.$scope.80", true)
                    editor.commit()
                } catch (_: SecurityException) {
                    // Permission was revoked; do not mark requested.
                }
            }
        }

        reading.apps.forEach { app -> send(app.id, app.name, app.foregroundMillis, false) }
        send(GoalHistoryPolicy.TOTAL, "전체 앱 사용", reading.totalMillis, true)
        val stale = prefs.all.keys.filter { key ->
            key.startsWith("goal_alert.") &&
                (key.removePrefix("goal_alert.").substringBefore('.').toLongOrNull()?.let { it < today - 45 } == true)
        }
        if (stale.isNotEmpty()) prefs.edit().also { editor -> stale.forEach(editor::remove) }.apply()
    }
}

class GoalAlertWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { GoalAlerts.checkNow(applicationContext) }
        Result.success()
    } catch (_: RuntimeException) { Result.retry() }
}
