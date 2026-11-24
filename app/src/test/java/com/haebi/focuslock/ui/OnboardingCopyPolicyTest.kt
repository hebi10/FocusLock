package com.haebi.focuslock.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingCopyPolicyTest {
    @Test fun analysisIsThePrimaryPurpose() {
        assertTrue(OnboardingCopyPolicy.introDescription.contains("Android"))
        assertTrue(OnboardingCopyPolicy.introDescription.contains("앱별 사용 시간"))
        assertTrue(OnboardingCopyPolicy.introDescription.contains("원한다면"))
        assertTrue(OnboardingCopyPolicy.goalDescription.contains("선택 사항"))
    }

    @Test fun permissionsAreUserControlledAndLocal() {
        assertTrue(OnboardingCopyPolicy.permissionDescription.contains("직접 허용"))
        assertTrue(OnboardingCopyPolicy.permissionNotice.contains("기기 안"))
        assertTrue(OnboardingCopyPolicy.permissionNotice.contains("화면 내용은 읽지 않"))
        assertTrue(OnboardingCopyPolicy.permissionNotice.contains("권한 없이도 집중 타이머"))
    }

    @Test fun appNeverPromisesForcedLocksOrExactAlerts() {
        assertFalse(OnboardingCopyPolicy.introDescription.contains("자동 차단"))
        assertTrue(OnboardingCopyPolicy.goalNotice.contains("늦어질 수"))
        assertTrue(OnboardingCopyPolicy.goalNotice.contains("강제로 차단하지 않"))
    }
}
