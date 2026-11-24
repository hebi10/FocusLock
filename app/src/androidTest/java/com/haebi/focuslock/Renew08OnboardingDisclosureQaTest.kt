package com.haebi.focuslock

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Renew08OnboardingDisclosureQaTest {
    @Test fun introductoryUserJourneyDescribesAnalysisAndOptionalGoals() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val previouslyCompleted = prefs.getBoolean("onboarding_completed", false)
        prefs.edit().putBoolean("onboarding_completed", false).commit()
        val device = UiDevice.getInstance(instrumentation)
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                assertTrue(device.wait(Until.hasObject(By.text("내 사용 습관을 한눈에")), 16000L))
                assertTrue(device.wait(
                    Until.hasObject(By.textContains("Android가 제공하는 앱별 사용 시간")), 8000L))
                val action = device.wait(Until.findObject(By.text("시작하기")), 6000L)
                requireNotNull(action).click()
                assertTrue(device.wait(Until.hasObject(By.text("사용 정보 접근")), 8000L))
                assertTrue(device.wait(
                    Until.hasObject(By.textContains("Android 설정에서 사용 정보 접근을 직접 허용")), 8000L))
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", previouslyCompleted).commit()
        }
    }
}
