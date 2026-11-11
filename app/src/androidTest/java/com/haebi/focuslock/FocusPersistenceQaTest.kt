package com.haebi.focuslock

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.haebi.focuslock.data.FocusClock
import com.haebi.focuslock.data.FocusDatabase
import com.haebi.focuslock.data.FocusSessionStatus
import com.haebi.focuslock.data.FocusSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusPersistenceQaTest {
    private class FixedClock : FocusClock {
        var monotonic = 10_000L
        var wall = 1_800_000_000_000L
        var boot = 5
        override fun monotonicMillis(): Long = monotonic
        override fun wallMillis(): Long = wall
        override fun bootCount(): Int = boot
    }

    @Test fun storedRewardSurvivesDatabaseReopenAndIsNotDuplicated() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "focuslock_003_qa.db"
        context.deleteDatabase(name)
        val clock = FixedClock()
        val original = Room.databaseBuilder(context, FocusDatabase::class.java, name).build()
        try {
            val store = FocusSessionStore(original, clock)
            val session = store.start(10)
            clock.monotonic += 600_000L
            clock.wall += 600_000L
            val first = store.finish(session.id)
            val second = store.finish(session.id)
            assertNotNull(first?.reward)
            assertEquals(first?.reward, second?.reward)
            assertEquals(30, store.totalExperience())
            assertEquals(1, store.report().last7.completedCount)
        } finally { original.close() }

        val reopened = Room.databaseBuilder(context, FocusDatabase::class.java, name).build()
        try {
            val store = FocusSessionStore(reopened, clock)
            assertEquals(30, store.totalExperience())
            assertEquals(1, store.report().last7.completedCount)
            assertEquals(null, store.restore().session)
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun todayExperienceIsLimitedToEarnedRewardsFromLocalDay() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java).build()
        try {
            val clock = FixedClock()
            val store = FocusSessionStore(db, clock)
            val session = store.start(10)
            clock.monotonic += 600_000L
            clock.wall += 600_000L
            store.finish(session.id)
            assertEquals(30, store.todayExperience())
            assertEquals(30, store.totalExperience())

            clock.wall += 86_400_000L
            assertEquals(0, store.todayExperience())
            assertEquals(30, store.totalExperience())
        } finally { db.close() }
    }

    @Test fun rebootPausesSessionAndNeverInventsCompletion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java).build()
        try {
            val clock = FixedClock()
            val store = FocusSessionStore(db, clock)
            val original = store.start(25)
            clock.monotonic += 20_000L
            val checkpoint = store.checkpoint(original.id)
            assertEquals(20, checkpoint?.elapsedSeconds)
            clock.boot++
            clock.monotonic = 5_000L
            val recovery = store.restore()
            assertTrue(recovery.pausedByRestart)
            assertEquals(FocusSessionStatus.PAUSED, recovery.session?.status)
            assertEquals(20, recovery.session?.elapsedSeconds)
            assertEquals(null, store.finish(original.id)?.reward)
            assertEquals(0, store.totalExperience())
        } finally { db.close() }
    }
}
