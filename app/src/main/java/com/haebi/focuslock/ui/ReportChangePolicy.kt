package com.haebi.focuslock.ui

import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageMetrics
import com.haebi.focuslock.data.UsageSnapshot
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ReportAppChange(
    val app: AppUsage,
    val differenceMillis: Long
)

internal object ReportChangePolicy {
    private const val MINUTE = 60_000L

    private fun hasComparableObservations(snapshot: UsageSnapshot, period: String): Boolean {
        if (period != "7일" && period != "30일") return false
        val coverage = snapshot.coverage ?: return false
        val current = snapshot.window(period)
        val prior = snapshot.prior(period)
        val periodDays = coverage.periodDays(current.startDate, current.endDate)
        if ((period == "7일" && periodDays != 7) ||
            (period == "30일" && periodDays != 30) ||
            coverage.periodDays(prior.startDate, prior.endDate) != periodDays) return false
        return coverage.observedDays(current.startDate, current.endDate) == periodDays &&
            coverage.observedDays(prior.startDate, prior.endDate) == periodDays
    }

    fun totalChange(snapshot: UsageSnapshot, period: String): String? {
        if (!hasComparableObservations(snapshot, period)) return null
        val current = snapshot.window(period).totalForegroundMillis
        val prior = snapshot.prior(period).totalForegroundMillis
        if (prior < 60 * MINUTE || current <= 0L) return null
        val difference = current - prior
        if (abs(difference) < 5 * MINUTE) return "이전 동일 기간과 사용 시간이 거의 같습니다."
        val rate = (abs(difference).toDouble() * 100 / prior.toDouble()).roundToInt()
        val direction = if (difference > 0L) "증가" else "감소"
        return "이전 " + period + "보다 " +
            UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(abs(difference))) +
            " (" + rate + "%) " + direction + "했습니다."
    }

    fun appChanges(
        snapshot: UsageSnapshot,
        period: String,
        excluded: Map<String, Boolean>
    ): List<ReportAppChange> {
        if (!hasComparableObservations(snapshot, period)) return emptyList()
        val olderById = snapshot.prior(period).apps.associateBy { it.id }
        return snapshot.window(period).apps.mapNotNull { app ->
            if (excluded[app.id] == true) return@mapNotNull null
            val earlier = olderById[app.id] ?: return@mapNotNull null
            val previous = earlier.foregroundMillis
            val current = app.foregroundMillis
            if (previous < 10 * MINUTE || current < MINUTE) return@mapNotNull null
            val difference = current - previous
            if (abs(difference) < 5 * MINUTE) return@mapNotNull null
            ReportAppChange(app, difference)
        }.sortedWith(
            compareByDescending<ReportAppChange> { abs(it.differenceMillis) }
                .thenBy { it.app.id }
        ).take(3)
    }

    fun appChangeLabel(change: ReportAppChange): String {
        val direction = if (change.differenceMillis > 0L) "증가" else "감소"
        return change.app.name + " · " +
            UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(abs(change.differenceMillis))) +
            " " + direction
    }

    const val sourceNotice =
        "Android 사용 통계의 관측된 날짜를 기준으로 비교합니다. " +
        "관측일이 있어도 데이터가 완전하다는 보장은 없으며, 표시하지 않은 앱을 0분으로 간주하지 않습니다."
}
