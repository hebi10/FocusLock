package com.haebi.focuslock.ui

import com.haebi.focuslock.data.GoalDayPolicy
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageMetrics
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.UsageWindow
import java.time.DayOfWeek
import kotlin.math.abs

internal object AppDetailPolicy {
    private val weekdayNames = listOf("월", "화", "수", "목", "금", "토", "일")
    private const val MINUTE = 60_000L

    fun activeDays(mask: Int): String {
        val normalized = GoalDayPolicy.normalize(mask)
        if (normalized == GoalDayPolicy.NONE) return "선택된 요일 없음"
        if (normalized == GoalDayPolicy.EVERY_DAY) return "매일"
        return DayOfWeek.entries
            .filter { GoalDayPolicy.selected(normalized, it) }
            .joinToString("·") { weekdayNames[it.value - 1] }
    }

    fun warningSummary(mask: Int, warn80: Boolean, warn100: Boolean): String {
        if (GoalDayPolicy.normalize(mask) == GoalDayPolicy.NONE) return "경고 알림 없음 (요일 미선택)"
        val thresholds = buildList {
            if (warn80) add("80%")
            if (warn100) add("100%")
        }
        if (thresholds.isEmpty()) return "경고 알림 사용 안 함"
        return "선택 요일에 " + thresholds.joinToString("·") + " 경고 설정"
    }

    fun appShare(window: UsageWindow, appId: String): Int? {
        val app = window.apps.firstOrNull { it.id == appId } ?: return null
        return UsageMetrics.rawSharePercent(
            app.foregroundMillis, window.totalForegroundMillis
        )
    }

    private fun comparable(snapshot: UsageSnapshot, period: String): Boolean {
        if (period != "7일" && period != "30일") return false
        val coverage = snapshot.coverage ?: return false
        val current = snapshot.window(period)
        val previous = snapshot.prior(period)
        val count = coverage.periodDays(current.startDate, current.endDate)
        if (count !in 7..30 ||
            coverage.periodDays(previous.startDate, previous.endDate) != count) return false
        return coverage.observedDays(current.startDate, current.endDate) == count &&
            coverage.observedDays(previous.startDate, previous.endDate) == count
    }

    fun periodComparison(snapshot: UsageSnapshot, appId: String, period: String): String? {
        if (!comparable(snapshot, period)) return null
        val current = snapshot.window(period).apps
            .firstOrNull { it.id == appId }?.foregroundMillis ?: return null
        val previous = snapshot.prior(period).apps
            .firstOrNull { it.id == appId }?.foregroundMillis ?: return null
        if (previous < 10 * MINUTE || current < MINUTE) return null
        val difference = current - previous
        if (abs(difference) < MINUTE) return "직전 " + period + "과 사용 시간 차이가 1분 미만입니다."
        val direction = if (difference > 0L) "증가" else "감소"
        return "직전 " + period + "보다 " +
            UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(abs(difference))) +
            " " + direction + "했습니다."
    }
}
