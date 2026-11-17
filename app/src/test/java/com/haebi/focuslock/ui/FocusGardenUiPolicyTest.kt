package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGardenUiPolicyTest {
    @Test fun timeNeverBecomesNegative() {
        assertEquals("00:00", FocusGardenUiPolicy.timerLabel(-1))
        assertEquals("01:05", FocusGardenUiPolicy.timerLabel(65))
        assertEquals("1분 05초", FocusGardenUiPolicy.elapsedLabel(65))
    }

    @Test fun restartAndPauseNoticesAreDistinct() {
        assertTrue(FocusGardenUiPolicy.restorationNotice(true, true).contains("재시작"))
        assertTrue(FocusGardenUiPolicy.restorationNotice(false, true).contains("일시정지"))
        assertTrue(FocusGardenUiPolicy.restorationNotice(false, false).contains("저장"))
        assertTrue(FocusGardenUiPolicy.resultNotice(true).contains("달성"))
        assertTrue(FocusGardenUiPolicy.resultNotice(false).contains("보상 조건"))
    }

    @Test fun onlyPersistedRewardsAppearAsEarned() {
        assertTrue(FocusGardenUiPolicy.rewardLine(0, 0).contains("지급된 보상 없음"))
        assertTrue(FocusGardenUiPolicy.rewardLine(30, 20).contains("경험치 +30"))
        assertFalse(FocusGardenUiPolicy.rewardLine(0, 0).contains("경험치 +"))
    }

    @Test fun gardenItemKeysAndUnlockMilestonesRemainCompatible() {
        assertEquals("tree", GardenCatalog.fromSaved("작은 나무").id)
        assertFalse(GardenCatalog.canPlace("tree", 149))
        assertTrue(GardenCatalog.canPlace("tree", 150))
        assertEquals("sprout", GardenCatalog.placed("tree", 149).id)
        assertEquals("tree", GardenCatalog.placed("tree", 150).id)
    }
}
