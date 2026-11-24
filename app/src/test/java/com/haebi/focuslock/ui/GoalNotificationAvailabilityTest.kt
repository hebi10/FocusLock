package com.haebi.focuslock.ui

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalNotificationAvailabilityTest {
    @Test fun runtimeAppAndChannelAreSeparateGates() {
        assertEquals(GoalNotificationAvailability.RUNTIME_DENIED,
            classifyGoalNotifications(false, true, NotificationManager.IMPORTANCE_DEFAULT))
        assertEquals(GoalNotificationAvailability.APP_BLOCKED,
            classifyGoalNotifications(true, false, NotificationManager.IMPORTANCE_DEFAULT))
        assertEquals(GoalNotificationAvailability.CHANNEL_MISSING, classifyGoalNotifications(true, true, null))
        assertEquals(GoalNotificationAvailability.CHANNEL_BLOCKED,
            classifyGoalNotifications(true, true, NotificationManager.IMPORTANCE_NONE))
        assertEquals(GoalNotificationAvailability.READY,
            classifyGoalNotifications(true, true, NotificationManager.IMPORTANCE_DEFAULT))
    }
}
