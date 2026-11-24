package com.haebi.focuslock.data

import java.time.LocalDate

internal data class GoalConfig(
    val minutes: Int,
    val days: Int = GoalDayPolicy.EVERY_DAY,
    val warn80: Boolean = false,
    val warn100: Boolean = false
)
internal data class GoalHistoryEvent(
    val scope: String,
    val date: LocalDate,
    val zoneId: String,
    val config: GoalConfig
)
internal enum class GoalDayResult { NO_GOAL, CHANGED, UNKNOWN, WITHIN, OVER }
internal data class GoalDayRecord(val date: LocalDate, val result: GoalDayResult, val limit: Int?, val millis: Long?)
internal data class GoalHistorySummaryData(val days: List<GoalDayRecord>) {
    val within get() = days.count { it.result == GoalDayResult.WITHIN }
    val over get() = days.count { it.result == GoalDayResult.OVER }
    val unknown get() = days.count { it.result == GoalDayResult.UNKNOWN || it.result == GoalDayResult.CHANGED }
    val observed get() = within + over
    val rate get() = if (observed == 0) null else (within * 100 + observed / 2) / observed
    val streak get() = days.asReversed().takeWhile { it.result == GoalDayResult.WITHIN }.size
}
internal object GoalHistoryPolicy {
    const val TOTAL = "@all"
    fun summarize(
        events: List<GoalHistoryEvent>,
        scope: String,
        today: LocalDate,
        zoneId: String?,
        observed: Map<LocalDate, Long>,
        days: Int
    ): GoalHistorySummaryData {
        val history = events.filter { it.scope == scope }.sortedBy { it.date }
        return GoalHistorySummaryData((days.coerceIn(1, 30) downTo 1).map { offset ->
            val date = today.minusDays(offset.toLong())
            val changed = history.any { it.date == date }
            val before = history.lastOrNull { it.date.isBefore(date) }
            val limit = before?.config?.minutes?.takeIf { it > 0 }
            val millis = observed[date]?.takeIf { it > 0 }
            val result = when {
                changed -> GoalDayResult.CHANGED
                limit == null -> GoalDayResult.NO_GOAL
                zoneId == null || before?.zoneId != zoneId || millis == null -> GoalDayResult.UNKNOWN
                millis > limit.toLong() * 60_000L -> GoalDayResult.OVER
                else -> GoalDayResult.WITHIN
            }
            GoalDayRecord(date, result, limit, millis)
        })
    }
}
