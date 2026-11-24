package com.haebi.focuslock.data

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object UsageTimePolicy {
    // Preserve the active offset during repeated local hours (DST fall-back).
    fun nextHourBoundary(cursorMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(cursorMillis).atZone(zone)
            .truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
}
