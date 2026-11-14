package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haebi.focuslock.data.FocusRewardRules
import com.haebi.focuslock.ui.FocusGardenUiPolicy
import com.haebi.focuslock.ui.GardenCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Renew07FocusGardenUiQaTest {
    @Test fun rewardAndGardenPersistenceRulesRemainConsistent() {
        assertFalse(FocusRewardRules.qualifies(25, 10))
        assertTrue(FocusRewardRules.qualifies(25, 1500))
        assertFalse(FocusRewardRules.qualifies(0, 599))
        assertTrue(FocusRewardRules.qualifies(0, 600))
        assertEquals("sprout", GardenCatalog.placed("tree", 149).id)
        assertEquals("tree", GardenCatalog.placed("tree", 150).id)
        assertTrue(FocusGardenUiPolicy.rewardLine(0, 0).contains("지급된 보상 없음"))
    }
}
