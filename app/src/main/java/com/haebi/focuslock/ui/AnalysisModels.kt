package com.haebi.focuslock.ui

import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageSnapshot
import java.text.Collator
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AnalysisBar(val label: String, val detail: String, val minutes: Int)

object AnalysisModels {
    val periods = listOf("오늘", "7일", "30일")
    val sorts = listOf("사용 시간", "실행 횟수", "이름")

    /** An unobserved event count is not a zero. Fall back to duration sorting. */
    fun effectiveSort(apps: List<AppUsage>, requested: String): String =
        if (requested == "실행 횟수" && apps.none { it.launches >= 0 }) "사용 시간"
        else requested

    fun visibleApps(
        apps: List<AppUsage>,
        excluded: Map<String, Boolean>,
        category: String,
        query: String,
        sort: String
    ): List<AppUsage> {
        val term = query.trim()
        val filtered = apps.filter { app ->
            excluded[app.id] != true &&
                (category == "전체" || app.category == category) &&
                (term.isEmpty() || app.name.contains(term, ignoreCase = true))
        }
        return when (effectiveSort(filtered, sort)) {
            "이름" -> {
                val collator = Collator.getInstance(Locale.KOREAN)
                filtered.sortedWith { left, right ->
                    val compare = collator.compare(left.name, right.name)
                    if (compare == 0) left.id.compareTo(right.id) else compare
                }
            }
            "실행 횟수" -> filtered.sortedWith(
                compareByDescending<AppUsage> { it.launches }.thenByDescending { it.minutes }
            )
            else -> filtered.sortedWith(
                compareByDescending<AppUsage> {
                    if (it.foregroundMillis > 0L) it.foregroundMillis else it.minutes.toLong() * 60_000L
                }.thenBy { it.name }
            )
        }
    }

    fun categories(apps: List<AppUsage>): List<String> =
        listOf("전체") + apps.map { it.category }.distinct().sorted()

    fun bars(snapshot: UsageSnapshot, period: String): List<AnalysisBar>? {
        val values = snapshot.chart(period) ?: return null
        val start = snapshot.window(period).startDate
        val dateDetail = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
        val shortDate = DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)
        val weekday = DateTimeFormatter.ofPattern("E", Locale.KOREAN)
        return values.mapIndexed { i, minutes ->
            if (period == "오늘") {
                AnalysisBar("${i}시", "${i}시", minutes)
            } else {
                val date = start.plusDays(i.toLong())
                AnalysisBar(
                    if (period == "7일") date.format(weekday) else date.format(shortDate),
                    date.format(dateDetail),
                    minutes
                )
            }
        }
    }
}
