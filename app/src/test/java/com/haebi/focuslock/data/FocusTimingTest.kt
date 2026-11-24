package com.haebi.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimingTest {
    @Test fun repeatedFractionalCheckpointsNeverLoseTime() {
        val origin = 10_000L
        var checkpoint = origin
        var saved = 0
        var now = origin
        repeat(59) {
            now += 10_007L
            val seconds = FocusTiming.seconds(FocusTiming.elapsedMillis(saved, checkpoint, now, 10))
            checkpoint = FocusTiming.nextCheckpoint(checkpoint, saved, seconds)
            saved = seconds
        }
        now = origin + 599_999
        val before = FocusTiming.elapsedMillis(saved, checkpoint, now, 10)
        assertEquals(599_999L, before)
        assertEquals(1, FocusTiming.remainingSeconds(10, before))
        assertFalse(FocusRewardRules.qualifies(10, FocusTiming.seconds(before)))
        val due = FocusTiming.elapsedMillis(saved, checkpoint, now + 1, 10)
        assertEquals(600_000L, due)
        assertEquals(0, FocusTiming.remainingSeconds(10, due))
        assertTrue(FocusRewardRules.qualifies(10, FocusTiming.seconds(due)))
        assertEquals(600_000L, FocusTiming.elapsedMillis(0, origin, now + 1, 10))
    }

    @Test fun runningAnchorAndPausedResumeShareSameDeadline() {
        val checkpoint = 50_000L
        val saved = 240
        val start = checkpoint - saved * 1000L
        val now = checkpoint + 60_000
        val persisted = FocusTiming.elapsedMillis(saved, checkpoint, now, 5)
        assertEquals(300_000L, persisted)
        assertEquals(persisted, FocusTiming.elapsedMillis(0, start, now, 5))
        assertEquals(0, FocusTiming.remainingSeconds(5, persisted))
        assertEquals(1000L, FocusTiming.nextTickDelay(persisted))
    }

    @Test fun freeFocusAndClockRollbackDoNotOverflow() {
        assertEquals(240_125L, FocusTiming.elapsedMillis(240, 500L, 625L, 0))
        assertEquals(240_000L, FocusTiming.elapsedMillis(240, 500L, 400L, 0))
        assertEquals(300_000L, FocusTiming.elapsedMillis(240, 500L, 900_000L, 5))
    }
}
