package com.haebi.focuslock.data

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTimePolicyTest {
    @Test fun repeatedHourUsesActualOffset() {
        val zone = ZoneId.of("America/New_York")
        val first = Instant.parse("2026-11-01T05:30:00Z").toEpochMilli()
        val second = Instant.parse("2026-11-01T06:30:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-01T06:00:00Z").toEpochMilli(), UsageTimePolicy.nextHourBoundary(first, zone))
        assertEquals(Instant.parse("2026-11-01T07:00:00Z").toEpochMilli(), UsageTimePolicy.nextHourBoundary(second, zone))
    }
    @Test fun midnightInLocalZoneAdvances() {
        val zone = ZoneId.of("Asia/Seoul")
        val cursor = Instant.parse("2026-11-24T14:59:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-24T15:00:00Z").toEpochMilli(), UsageTimePolicy.nextHourBoundary(cursor, zone))
    }
}
