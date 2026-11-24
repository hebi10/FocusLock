package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haebi.focuslock.data.GoalDayPolicy
import com.haebi.focuslock.ui.AppDetailPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Renew05AppDetailPolicyQaTest {
    @Test fun existingGoalDaysAndRecordedComparisonRemainHonest() {
        assertEquals("매일", AppDetailPolicy.activeDays(GoalDayPolicy.EVERY_DAY))
        assertEquals("경고 알림 없음 (요일 미선택)",
            AppDetailPolicy.warningSummary(GoalDayPolicy.NONE, true, true))
        assertNull(AppDetailPolicy.appShare(
            com.haebi.focuslock.data.UsageWindow(
                emptyList(), 0, java.time.LocalDate.now(), java.time.LocalDate.now(), 0L
            ), "unknown"
        ))
    }
}
