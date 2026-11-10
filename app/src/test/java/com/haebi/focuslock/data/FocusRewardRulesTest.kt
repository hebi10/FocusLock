package com.haebi.focuslock.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRewardRulesTest {
    @Test fun timedRewardNeedsAllSeconds() {
        assertFalse(FocusRewardRules.qualifies(25, 1499))
        assertTrue(FocusRewardRules.qualifies(25, 1500))
    }

    @Test fun freeFocusNeedsTenMinutes() {
        assertFalse(FocusRewardRules.qualifies(0, 599))
        assertTrue(FocusRewardRules.qualifies(0, 600))
    }
}
