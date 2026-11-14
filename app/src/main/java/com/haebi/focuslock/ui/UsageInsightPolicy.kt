package com.haebi.focuslock.ui

import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageMetrics
import com.haebi.focuslock.data.UsageSnapshot
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ObservedInsight(
    val id: String,
    val message: String,
    val source: String
)
internal data class GoalInsightSuggestion(val app: AppUsage, val insight: ObservedInsight)

internal object UsageInsightPolicy {
    private const val MINUTE = 60_000L

    fun todayTop(snapshot: UsageSnapshot, excluded: Map<String, Boolean>): ObservedInsight? {
        val window = snapshot.today
        if (window.totalForegroundMillis < MINUTE) return null
        val app = window.apps.filter { excluded[it.id] != true && it.foregroundMillis >= MINUTE }
            .maxWithOrNull(compareBy<AppUsage> { it.foregroundMillis }.thenBy { it.id })
            ?: return null
        val share = UsageMetrics.rawSharePercent(
            app.foregroundMillis, window.totalForegroundMillis
        ) ?: return null
        return ObservedInsight(
            "I01",
            "오늘 가장 오래 사용한 앱은 " + app.name + " (" +
                UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(app.foregroundMillis)) +
                ", 전체의 " + share + "%)입니다.",
            "Android 앱 사용 시간 기준 · 분석 제외 앱도 전체 합계에는 포함"
        )
    }

    fun todayPeak(snapshot: UsageSnapshot): ObservedInsight? {
        val hourly = snapshot.hourlyToday ?: return null
        if (hourly.size != 24) return null
        val peak = hourly.maxOrNull() ?: return null
        if (peak < 5 || snapshot.today.totalForegroundMillis < 5 * MINUTE) return null
        val hour = hourly.indexOf(peak)
        if (hour !in 0..23) return null
        return ObservedInsight(
            "I02",
            "오늘 관측된 이벤트에서는 " + hour + "시~" + (hour + 1) +
                "시의 사용 시간이 가장 길었습니다 (" + peak + "분).",
            "Android 앱 전환 이벤트 기준 관측값 · 일부 이벤트 누락 가능"
        )
    }

    private fun comparable(snapshot: UsageSnapshot, period: String): Boolean {
        val coverage = snapshot.coverage ?: return false
        val current = snapshot.window(period)
        val previous = snapshot.prior(period)
        val days = coverage.periodDays(current.startDate, current.endDate)
        if (days !in 7..30 || coverage.periodDays(previous.startDate, previous.endDate) != days) {
            return false
        }
        return coverage.observedDays(current.startDate, current.endDate) == days &&
            coverage.observedDays(previous.startDate, previous.endDate) == days
    }

    private fun verifiedWeeklyChanges(
        snapshot: UsageSnapshot,
        excluded: Map<String, Boolean>
    ): List<Pair<AppUsage, Long>> {
        if (!comparable(snapshot, "7일")) return emptyList()
        val previousById = snapshot.previous7.apps.associateBy { it.id }
        return snapshot.last7.apps.mapNotNull { app ->
            if (excluded[app.id] == true) return@mapNotNull null
            val previous = previousById[app.id] ?: return@mapNotNull null
            if (previous.foregroundMillis < 10 * MINUTE || app.foregroundMillis < MINUTE) {
                return@mapNotNull null
            }
            val delta = app.foregroundMillis - previous.foregroundMillis
            if (abs(delta) < 5 * MINUTE) return@mapNotNull null
            app to delta
        }.sortedWith(
            compareByDescending<Pair<AppUsage, Long>> { abs(it.second) }
                .thenBy { it.first.id }
        )
    }

    fun weeklyChanges(
        snapshot: UsageSnapshot,
        excluded: Map<String, Boolean>
    ): List<ObservedInsight> = verifiedWeeklyChanges(snapshot, excluded).take(3)
        .map { (app, delta) ->
            val direction = if (delta > 0L) "증가" else "감소"
            ObservedInsight(
                "I03",
                "최근 7일 " + app.name + " 사용 시간이 이전 7일보다 " +
                    UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(abs(delta))) +
                    " " + direction + "했습니다.",
                "Android 앱별 실측 기록과 양 기간 관측일 기준 · 일부 기록 누락 가능"
            )
        }

    fun totalChange(snapshot: UsageSnapshot, period: String): ObservedInsight? {
        if (period != "7일" && period != "30일") return null
        if (!comparable(snapshot, period)) return null
        val current = snapshot.window(period).totalForegroundMillis
        val previous = snapshot.prior(period).totalForegroundMillis
        if (previous < 60 * MINUTE || current <= 0L) return null
        val delta = current - previous
        if (abs(delta) < 5 * MINUTE) return null
        val percentage = (abs(delta).toDouble() / previous.toDouble() * 100).roundToInt()
        val direction = if (delta > 0L) "증가" else "감소"
        return ObservedInsight(
            "I04",
            "최근 " + period + " 총 사용 시간이 직전 동일 기간보다 " +
                percentage + "% " + direction + "했습니다.",
            "Android 두 기간 실측 사용 시간 기준 · 관측일 확인은 기록의 완전성을 보장하지 않음"
        )
    }

    fun frequentApps(
        snapshot: UsageSnapshot,
        excluded: Map<String, Boolean>
    ): ObservedInsight? {
        val apps = snapshot.last7.apps
            .filter { excluded[it.id] != true && it.launches > 0 }
            .sortedWith(compareByDescending<AppUsage> { it.launches }.thenBy { it.id })
            .take(3)
        if (apps.isEmpty()) return null
        return ObservedInsight(
            "I05",
            "최근 7일 확인된 실행·전환 이벤트 상위 앱: " +
                apps.joinToString(", ") { it.name + " " + it.launches + "회" },
            "Android ACTIVITY_RESUMED 관측값 · 전체 실행 횟수는 아닐 수 있음"
        )
    }

    fun goalSuggestion(
        snapshot: UsageSnapshot,
        goals: Map<String, Int>,
        excluded: Map<String, Boolean>
    ): GoalInsightSuggestion? {
        val candidate = verifiedWeeklyChanges(snapshot, excluded)
            .firstOrNull { (app, delta) ->
                delta >= 15 * MINUTE && (goals[app.id] ?: 0) <= 0
            } ?: return null
        val (app, delta) = candidate
        return GoalInsightSuggestion(
            app,
            ObservedInsight(
                "I06",
                app.name + "의 최근 7일 사용 시간이 이전 7일보다 " +
                    UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(delta)) +
                    " 늘었습니다. 원한다면 사용 목표를 설정할 수 있습니다.",
                "실제 사용량에 근거한 선택형 제안 · 자동 차단이나 건강 진단이 아님"
            )
        )
    }

    fun forPeriod(
        snapshot: UsageSnapshot,
        period: String,
        excluded: Map<String, Boolean>
    ): List<ObservedInsight> = when (period) {
        "오늘" -> listOfNotNull(todayTop(snapshot, excluded), todayPeak(snapshot))
        "7일" -> weeklyChanges(snapshot, excluded) +
            listOfNotNull(totalChange(snapshot, "7일"), frequentApps(snapshot, excluded))
        "30일" -> listOfNotNull(totalChange(snapshot, "30일"))
        else -> emptyList()
    }
}
