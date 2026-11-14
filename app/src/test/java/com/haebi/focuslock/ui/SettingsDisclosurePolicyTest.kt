package com.haebi.focuslock.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDisclosurePolicyTest {
    @Test fun existingSettingsDescribeOnlyCurrentCapabilities() {
        assertTrue(SettingsDisclosurePolicy.subtitle.contains("사용 분석"))
        assertTrue(SettingsDisclosurePolicy.alertNotice.contains("15분"))
        assertTrue(SettingsDisclosurePolicy.alertNotice.contains("늦어질 수"))
        assertTrue(SettingsDisclosurePolicy.limitsNotice.contains("자동 차단"))
        assertTrue(SettingsDisclosurePolicy.limitsNotice.contains("보장하지 않"))
    }

    @Test fun privacyAndDeletionCopyPreserveLocalDataMeaning() {
        assertTrue(SettingsDisclosurePolicy.localDataNotice.contains("이 기기"))
        assertTrue(SettingsDisclosurePolicy.localDataNotice.contains("메시지나 화면 내용은 읽지 않"))
        assertTrue(SettingsDisclosurePolicy.deleteNotice.contains("목표·집중·보상·정원"))
        assertTrue(SettingsDisclosurePolicy.deleteNotice.contains("별도로 관리"))
        assertTrue(SettingsDisclosurePolicy.privacyNotice.contains("현재 구현된 기능"))
        assertFalse(SettingsDisclosurePolicy.localDataNotice.contains("클라우드 자동 백업"))
    }
}
