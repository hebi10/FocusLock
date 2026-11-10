package com.haebi.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalAlertPolicyTest {
    @Test fun sends80BeforeFullGoal() {
        assertNull(GoalAlertPolicy.threshold(47, 60, true, true))
        assertEquals(80, GoalAlertPolicy.threshold(48, 60, true, true))
    }

    @Test fun sendsOnlyHighestPendingThreshold() {
        assertEquals(100, GoalAlertPolicy.threshold(61, 60, true, true))
        assertEquals(80, GoalAlertPolicy.threshold(61, 60, true, false))
        assertEquals(100, GoalAlertPolicy.threshold(61, 60, false, true))
        assertNull(GoalAlertPolicy.threshold(61, 60, false, false))
    }

    @Test fun rejectsUnconfiguredGoal() {
        assertNull(GoalAlertPolicy.threshold(10, 0, true, true))
        assertNull(GoalAlertPolicy.threshold(0, 60, true, true))
    }
}
