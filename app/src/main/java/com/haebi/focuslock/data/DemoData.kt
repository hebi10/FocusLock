package com.haebi.focuslock.data

import kotlin.math.roundToInt

/** Design fixture only. Replace with permission-gated UsageStatsManager records in a later stage. */
data class AppUsage(
    val id: String,
    val name: String,
    val category: String,
    val minutes: Int,
    val launches: Int,
    val lastUsed: String,
    val symbol: String,
    val tint: Long,
    val packageName: String? = null,
    val foregroundMillis: Long = 0L
)

object DemoData {
    val apps = listOf(
        AppUsage("youtube", "YouTube", "동영상", 58, 6, "10:08", "▶", 0xFFD92F37, "com.google.android.youtube"),
        AppUsage("instagram", "Instagram", "SNS", 36, 8, "09:47", "◎", 0xFF9B59B6, "com.instagram.android"),
        AppUsage("games", "게임", "게임", 31, 4, "09:20", "✚", 0xFF8462D4),
        AppUsage("chrome", "Chrome", "업무", 10, 5, "08:55", "C", 0xFF3976C6, "com.android.chrome")
    )

    val dailyUsageMinutes = listOf(125, 110, 130, 100, 145, 125, 125)
    val hourlyUsageMinutes = listOf(0, 0, 0, 0, 0, 0, 0, 0, 3, 9, 6, 9, 12, 18, 9, 6, 7, 10, 25, 12, 6, 2, 1, 0)
    val categories = listOf("동영상", "SNS", "게임", "업무")
    val dailyGoalMinutes = 180
    val monthlyUsageMinutes = List(30) { dailyUsageMinutes[it % 7] }
    val monthlyTotalMinutes: Int get() = monthlyUsageMinutes.sum()

    val todayTotalMinutes: Int get() = apps.sumOf { it.minutes }
    val weeklyTotalMinutes: Int get() = dailyUsageMinutes.sum()

    fun usageByPeriod(period: String): List<AppUsage> {
        if (period == "오늘") return apps
        val target = if (period == "7일") weeklyTotalMinutes else monthlyTotalMinutes
        var assigned = 0
        return apps.mapIndexed { index, app ->
            val minutes = if (index == apps.lastIndex) target - assigned
                else (app.minutes.toDouble() / todayTotalMinutes * target).roundToInt()
            assigned += minutes
            app.copy(minutes = minutes)
        }
    }

    fun minutesLabel(totalMinutes: Int): String = when {
        totalMinutes < 60 -> "${totalMinutes}분"
        totalMinutes % 60 == 0 -> "${totalMinutes / 60}시간"
        else -> "${totalMinutes / 60}시간 ${totalMinutes % 60}분"
    }

    fun sharePercent(minutes: Int, totalMinutes: Int): Int =
        if (totalMinutes <= 0) 0 else (100.0 * minutes / totalMinutes).roundToInt()

    fun appById(id: String): AppUsage = apps.firstOrNull { it.id == id } ?: apps.first()
}
