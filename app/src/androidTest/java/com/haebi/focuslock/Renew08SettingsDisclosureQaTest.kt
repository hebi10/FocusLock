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
class Renew08SettingsDisclosureQaTest {
    @Test
    fun settingsShowLocalDataAndHonestLimits() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val originallyCompleted = prefs.getBoolean("onboarding_completed", false)
        prefs.edit().putBoolean("onboarding_completed", true).commit()
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                assertTrue(device.wait(Until.hasObject(By.text("오늘의 FocusLock")), 20000L))
                val settings = device.wait(Until.findObject(By.desc("설정")), 4000L)
                    ?: device.wait(Until.findObject(By.text("설정")), 4000L)
                requireNotNull(settings).click()
                assertTrue(device.wait(Until.hasObject(By.text("사용 정보·알림 권한 관리")), 8000L))
                assertTrue(device.wait(Until.hasObject(By.text("화면")), 8000L))
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", originallyCompleted).commit()
        }
    }
}
