package com.haebi.focuslock.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserManager
import android.os.Build
import com.haebi.focuslock.ui.hasUsageAccess
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

sealed interface UsageLoadState {
    data object Loading : UsageLoadState
    data object PermissionRequired : UsageLoadState
    data class Unavailable(val reason: String) : UsageLoadState
    data class Ready(val snapshot: UsageSnapshot) : UsageLoadState
}

data class UsageWindow(
    val apps: List<AppUsage>,
    val totalMinutes: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class UsageSnapshot(
    val today: UsageWindow,
    val last7: UsageWindow,
    val last30: UsageWindow,
    val yesterday: UsageWindow,
    val previous7: UsageWindow,
    val previous30: UsageWindow,
    val daily7: List<Int>,
    val daily30: List<Int>,
    val appDaily7: Map<String, List<Int>>,
    val hourlyToday: List<Int>?,
    val collectedAtMillis: Long
) {
    fun window(period: String): UsageWindow = when (period) {
        "7일" -> last7
        "30일" -> last30
        else -> today
    }

    fun prior(period: String): UsageWindow = when (period) {
        "7일" -> previous7
        "30일" -> previous30
        else -> yesterday
    }

    fun chart(period: String): List<Int>? = when (period) {
        "7일" -> daily7
        "30일" -> daily30
        else -> hourlyToday
    }
}

object UsageFormat {
    fun minutesLabel(value: Int): String = when {
        value < 60 -> "${value}분"
        value % 60 == 0 -> "${value / 60}시간"
        else -> "${value / 60}시간 ${value % 60}분"
    }

    fun sharePercent(minutes: Int, total: Int): Int = if (total <= 0) 0 else
        (100.0 * minutes / total).roundToInt()

    fun comparison(current: Int, previous: Int): String? {
        if (previous <= 0) return null
        val diff = current - previous
        return when {
            diff < 0 -> "이전 기간보다 ${minutesLabel(-diff)} 감소"
            diff > 0 -> "이전 기간보다 ${minutesLabel(diff)} 증가"
            else -> "이전 기간과 동일"
        }
    }
}

private data class DailySample(
    val packageName: String,
    val day: LocalDate,
    val foregroundMillis: Long,
    val lastTimeUsed: Long
)

private data class LaunchKey(val day: LocalDate, val pkg: String)

/** Reads the OS-owned usage store. No synthetic usage values or remote transmission. */
class UsageRepository(private val context: Context) {
    private val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packages = context.packageManager
    private val zone = ZoneId.systemDefault()
    private val fmt = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)

    fun load(nowMillis: Long = System.currentTimeMillis()): UsageLoadState {
        if (!hasUsageAccess(context)) return UsageLoadState.PermissionRequired
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        if (!userManager.isUserUnlocked) return UsageLoadState.Unavailable("기기 잠금을 해제한 뒤 사용 기록을 확인해 주세요.")
        return try {
            val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            val firstDay = today.minusDays(59)
            val startMillis = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMillis, nowMillis)
                ?: return UsageLoadState.Unavailable("Android에서 사용 통계를 제공하지 않았습니다. 기기 잠금 상태와 권한을 확인해 주세요.")
            val rows = stats.filter { it.packageName != context.packageName && it.totalTimeInForeground > 0 }
                .mapNotNull { stat ->
                    val date = Instant.ofEpochMilli(stat.firstTimeStamp).atZone(zone).toLocalDate()
                    if (date.isBefore(firstDay) || date.isAfter(today)) null
                    else DailySample(stat.packageName, date, stat.totalTimeInForeground, stat.lastTimeUsed)
                }
            val latestEventDay = today.minusDays(6)
            val eventStart = latestEventDay.atStartOfDay(zone).toInstant().toEpochMilli()
            val events = manager.queryEvents(eventStart, nowMillis)
            val launched = mutableMapOf<LaunchKey, Int>()
            val lastLaunch = mutableMapOf<String, Long>()
            val hourlyMillis = LongArray(24)
            var previousResumed: String? = null
            var previousTime = eventStart
            var sawAnyEvents = false
            val resumedType = UsageEvents.Event.ACTIVITY_RESUMED
            val pausedType = UsageEvents.Event.ACTIVITY_PAUSED
            if (events != null) {
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val timestamp = event.timeStamp.coerceIn(eventStart, nowMillis)
                    val packageName = event.packageName ?: continue
                    // Keep our own foreground transitions in the timeline to close the previous app's interval.
                    // appendHourly intentionally excludes FocusLock itself.
                    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                    when (event.eventType) {
                        resumedType -> {
                            sawAnyEvents = true
                            if (previousResumed != null) appendHourly(previousTime, timestamp, previousResumed!!, today, hourlyMillis)
                            val key = LaunchKey(day, packageName)
                            val last = lastLaunch[packageName]
                            if (last == null || timestamp - last > 1500L) {
                                launched[key] = (launched[key] ?: 0) + 1
                            }
                            lastLaunch[packageName] = timestamp
                            previousResumed = packageName
                            previousTime = timestamp
                        }
                        pausedType -> if (packageName == previousResumed) {
                            appendHourly(previousTime, timestamp, packageName, today, hourlyMillis)
                            previousResumed = null
                        }
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> {
                            if (previousResumed != null) appendHourly(previousTime, timestamp, previousResumed!!, today, hourlyMillis)
                            previousResumed = null
                        }
                    }
                }
                if (previousResumed != null) appendHourly(previousTime, nowMillis, previousResumed!!, today, hourlyMillis)
            }
            val appMeta = mutableMapOf<String, AppMeta>()
            val window: (Int, Int, Boolean) -> UsageWindow = { from, to, includeEvents ->
                val start = today.minusDays(from.toLong())
                val end = today.minusDays(to.toLong())
                val grouped = rows.filter { !it.day.isBefore(start) && !it.day.isAfter(end) }
                    .groupBy { it.packageName }
                val apps = grouped.map { (pkg, samples) ->
                    val millis = samples.sumOf { it.foregroundMillis }
                    val meta = appMeta.getOrPut(pkg) { describeApp(pkg) }
                    val date = samples.maxOf { it.lastTimeUsed }
                    val count = if (includeEvents && events != null) {
                        launched.filterKeys { it.pkg == pkg && !it.day.isBefore(start) && !it.day.isAfter(end) }.values.sum()
                    } else -1
                    AppUsage(
                        id = pkg, name = meta.name, category = meta.category,
                        minutes = millisToMinutes(millis), launches = count,
                        lastUsed = if (date > 0L) Instant.ofEpochMilli(date).atZone(zone).format(fmt) else "확인 불가",
                        symbol = meta.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        tint = 0xFF666666, packageName = pkg
                    )
                }.filter { it.minutes > 0 }.sortedByDescending { it.minutes }
                UsageWindow(apps, apps.sumOf { it.minutes }, start, end)
            }
            val todayWindow = window(0, 0, true)
            val last7 = window(6, 0, true)
            val last30 = window(29, 0, false)
            val yesterday = window(1, 1, true)
            val previous7 = window(13, 7, false)
            val previous30 = window(59, 30, false)
            fun dayMinutes(day: LocalDate): Int = millisToMinutes(rows.filter { it.day == day }.sumOf { it.foregroundMillis })
            val daily7 = (6 downTo 0).map { dayMinutes(today.minusDays(it.toLong())) }
            val daily30 = (29 downTo 0).map { dayMinutes(today.minusDays(it.toLong())) }
            val appDaily7 = rows.groupBy { it.packageName }.mapValues { (_, appRows) ->
                (6 downTo 0).map { offset ->
                    millisToMinutes(appRows.filter { it.day == today.minusDays(offset.toLong()) }.sumOf { it.foregroundMillis })
                }
            }
            val hourly = if (sawAnyEvents) hourlyMillis.map(::millisToMinutes) else null
            UsageLoadState.Ready(UsageSnapshot(todayWindow, last7, last30, yesterday, previous7, previous30, daily7, daily30, appDaily7, hourly, nowMillis))
        } catch (_: SecurityException) {
            UsageLoadState.PermissionRequired
        } catch (_: RuntimeException) {
            UsageLoadState.Unavailable("사용 기록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.")
        }
    }

    private fun appendHourly(start: Long, end: Long, pkg: String, today: LocalDate, hours: LongArray) {
        if (end <= start || pkg == context.packageName) return
        var cursor = maxOf(start, today.atStartOfDay(zone).toInstant().toEpochMilli())
        while (cursor < end) {
            val local = Instant.ofEpochMilli(cursor).atZone(zone)
            val hour = local.hour
            val nextHour = local.toLocalDate().atTime(hour, 0).atZone(zone).plusHours(1).toInstant().toEpochMilli()
            val until = minOf(end, nextHour)
            if (until <= cursor) break
            hours[hour] += until - cursor
            cursor = until
        }
    }

    private data class AppMeta(val name: String, val category: String)

    private fun describeApp(packageName: String): AppMeta = try {
        val appInfo = if (Build.VERSION.SDK_INT >= 33) {
            packages.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packages.getApplicationInfo(packageName, 0)
        }
        val category = when (appInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> "게임"
            ApplicationInfo.CATEGORY_VIDEO -> "동영상"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "업무"
            ApplicationInfo.CATEGORY_SOCIAL -> "SNS"
            else -> "기타"
        }
        AppMeta(packages.getApplicationLabel(appInfo).toString(), category)
    } catch (_: PackageManager.NameNotFoundException) {
        AppMeta(packageName, "기타")
    } catch (_: SecurityException) {
        AppMeta(packageName, "기타")
    }

    private fun millisToMinutes(millis: Long): Int = (millis.coerceAtLeast(0L) / 60000.0).roundToInt().coerceAtLeast(0)
}
