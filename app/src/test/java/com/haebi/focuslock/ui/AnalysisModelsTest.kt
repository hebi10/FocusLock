package com.haebi.focuslock.ui

import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.UsageWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalysisModelsTest {
    private val today = LocalDate.of(2026, 11, 11)

    private fun app(id: String, name: String, category: String, minutes: Int, launches: Int) =
        AppUsage(id, name, category, minutes, launches, "확인 불가", name.take(1), 0xFF666666)

    private val apps = listOf(
        app("video", "YouTube", "동영상", 58, 6),
        app("social", "인스타그램", "SNS", 36, -1),
        app("browser", "Chrome", "업무", 12, 9),
        app("game", "게임", "게임", 8, 4)
    )

    @Test
    fun searchTrimCaseAndCategoryExcludeAreAppliedTogether() {
        val filtered = AnalysisModels.visibleApps(apps, mapOf("game" to true), "동영상", "  youtube ", "사용 시간")
        assertEquals(listOf("video"), filtered.map { it.id })
        assertTrue(AnalysisModels.visibleApps(apps, emptyMap(), "전체", "no-match", "사용 시간").isEmpty())
        assertFalse(AnalysisModels.categories(apps).contains("모름"))
    }

    @Test
    fun sortByNameAndLaunchesKeepsUnknownCountsLast() {
        assertEquals(listOf("browser", "video", "game", "social"),
            AnalysisModels.visibleApps(apps, emptyMap(), "전체", "", "이름").map { it.id })
        assertEquals(listOf("browser", "video", "game", "social"),
            AnalysisModels.visibleApps(apps, emptyMap(), "전체", "", "실행 횟수").map { it.id })
        assertEquals(listOf("video", "social", "browser", "game"),
            AnalysisModels.visibleApps(apps, emptyMap(), "전체", "", "사용 시간").map { it.id })
    }

    @Test
    fun launchEventSortNeverInventsUnavailableCounts() {
        val unknown = apps.map { it.copy(launches = -1) }
        assertEquals("사용 시간", AnalysisModels.effectiveSort(unknown, "실행 횟수"))
        assertEquals("실행 횟수", AnalysisModels.effectiveSort(apps, "실행 횟수"))
        assertEquals("이름", AnalysisModels.effectiveSort(unknown, "이름"))
        assertEquals(
            listOf("video", "social", "browser", "game"),
            AnalysisModels.visibleApps(unknown, emptyMap(), "전체", "", "실행 횟수")
                .map { it.id }
        )
    }

    @Test
    fun durationSortUsesOriginalOSMillisWhenRoundedMinutesAreEqual() {
        val nearMinute = listOf(
            app("a", "앱 A", "기타", 1, -1).copy(foregroundMillis = 32_000L),
            app("b", "앱 B", "기타", 1, -1).copy(foregroundMillis = 58_000L)
        )
        assertEquals(
            listOf("b", "a"),
            AnalysisModels.visibleApps(nearMinute, emptyMap(), "전체", "", "사용 시간")
                .map { it.id }
        )
    }

    private fun window(from: LocalDate, to: LocalDate) = UsageWindow(apps, apps.sumOf { it.minutes }, from, to)
    private val snapshot: UsageSnapshot
        get() = UsageSnapshot(
            today = window(today, today),
            last7 = window(today.minusDays(6), today),
            last30 = window(today.minusDays(29), today),
            yesterday = window(today.minusDays(1), today.minusDays(1)),
            previous7 = window(today.minusDays(13), today.minusDays(7)),
            previous30 = window(today.minusDays(59), today.minusDays(30)),
            daily7 = listOf(2, 4, 6, 8, 10, 12, 14),
            daily30 = List(30) { it },
            appDaily7 = emptyMap(),
            hourlyToday = List(24) { if (it == 14) 3 else 0 },
            collectedAtMillis = 0
        )

    @Test
    fun chartLabelsFollowSnapshotWindowNotWallClock() {
        val hourly = AnalysisModels.bars(snapshot, "오늘")!!
        assertEquals(24, hourly.size)
        assertEquals("14시", hourly[14].detail)
        assertEquals(3, hourly[14].minutes)

        val weekly = AnalysisModels.bars(snapshot, "7일")!!
        assertEquals(7, weekly.size)
        assertEquals("11월 5일 목요일", weekly.first().detail)
        assertEquals("11월 11일 수요일", weekly.last().detail)

        val monthly = AnalysisModels.bars(snapshot, "30일")!!
        assertEquals(30, monthly.size)
        assertEquals("10월 13일 화요일", monthly.first().detail)
    }

    @Test
    fun unavailableHourlyEventsAreNotReplacedWithFakeZeros() {
        assertNull(AnalysisModels.bars(snapshot.copy(hourlyToday = null), "오늘"))
    }
}
