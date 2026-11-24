package com.haebi.focuslock.ui

import com.haebi.focuslock.data.UsageSnapshot

internal object UsageComparisonPolicy {
    const val partialTodayNotice = "최근 기간은 오늘 현재까지 포함하며, 직전 기간은 전체 날짜입니다."
    const val foregroundNotice = "Android 앱별 포그라운드 시간 합계입니다. 동시 사용 시 실제 경과 시간과 다를 수 있습니다."

    fun hasComparablePeriods(snapshot: UsageSnapshot, period: String): Boolean {
        val days = when (period) { "7일" -> 7; "30일" -> 30; else -> return false }
        val coverage = snapshot.coverage ?: return false
        val current = snapshot.window(period)
        val previous = snapshot.prior(period)
        if (coverage.periodDays(current.startDate, current.endDate) != days ||
            coverage.periodDays(previous.startDate, previous.endDate) != days ||
            previous.endDate.plusDays(1) != current.startDate) return false
        return coverage.observedDays(current.startDate, current.endDate) == days &&
            coverage.observedDays(previous.startDate, previous.endDate) == days
    }

    fun summary(snapshot: UsageSnapshot, period: String): String =
        if (period == "오늘") "오늘은 진행 중입니다. 어제 전체 사용 시간과 직접 비교하지 않습니다."
        else ReportChangePolicy.totalChange(snapshot, period)
            ?: "이전 기간의 관측 기록이 부족해 정확한 변화를 계산할 수 없습니다."
}
