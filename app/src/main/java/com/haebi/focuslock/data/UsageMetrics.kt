package com.haebi.focuslock.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * An observed day means Android returned positive foreground time for at least one app.
 * Missing dates cannot be classified as zero-usage days or missing OS history.
 */
data class UsageCoverage(val observedDates: Set<LocalDate>) {
    fun observedDays(start: LocalDate, end: LocalDate): Int =
        if (end.isBefore(start)) 0 else observedDates.count { !it.isBefore(start) && !it.isAfter(end) }

    fun periodDays(start: LocalDate, end: LocalDate): Int =
        if (end.isBefore(start)) 0 else
            (ChronoUnit.DAYS.between(start, end) + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun description(start: LocalDate, end: LocalDate): String {
        val total = periodDays(start, end)
        val observed = observedDays(start, end)
        return "선택 기간 ${total}일 중 사용 기록이 확인된 날 ${observed}일"
    }
}

object UsageMetrics {
    fun roundedMinutes(millis: Long): Int =
        (millis.coerceAtLeast(0L).toDouble() / 60_000.0)
            .roundToLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    /** Aggregate original OS milliseconds before one display rounding operation. */
    fun totalMillis(values: Iterable<Long>): Long {
        var total = 0L
        values.forEach { value ->
            if (value > 0L) {
                total = if (Long.MAX_VALUE - total < value) Long.MAX_VALUE else total + value
            }
        }
        return total
    }

    fun totalMinutes(values: Iterable<Long>): Int = roundedMinutes(totalMillis(values))

    fun rawSharePercent(partMillis: Long, totalMillis: Long): Int? {
        if (partMillis <= 0L || totalMillis <= 0L || partMillis > totalMillis) return null
        return (partMillis.toDouble() / totalMillis * 100).roundToInt().coerceIn(0, 100)
    }
}
