package com.haebi.focuslock.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

interface FocusClock {
    fun monotonicMillis(): Long
    fun wallMillis(): Long
    fun bootCount(): Int
}

class AndroidFocusClock(private val context: Context) : FocusClock {
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
    override fun wallMillis(): Long = System.currentTimeMillis()
    override fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
}

object FocusRewardRules {
    fun qualifies(durationMinutes: Int, elapsedSeconds: Int): Boolean =
        if (durationMinutes == 0) elapsedSeconds >= 600
        else elapsedSeconds >= durationMinutes * 60
}

data class FocusRestore(val session: FocusSessionEntity?, val pausedByRestart: Boolean = false)
data class FocusFinish(val session: FocusSessionEntity, val reward: FocusRewardEntity?)

data class FocusReport(val completedCount: Int = 0, val completedSeconds: Int = 0, val experience: Int = 0) {
    val minutes: Int get() = completedSeconds / 60
}
data class FocusSummary(
    val last7: FocusReport = FocusReport(),
    val last30: FocusReport = FocusReport()
) {
    fun forPeriod(period: String): FocusReport = if (period == "30일") last30 else last7
}

/** Room is the source of truth. All state transitions are atomic, including one-time rewards. */
class FocusSessionStore(
    private val database: FocusDatabase,
    private val clock: FocusClock
) {
    private val dao = database.focusDao()

    companion object {
        fun onDevice(context: Context): FocusSessionStore =
            FocusSessionStore(FocusDatabase.get(context), AndroidFocusClock(context))
    }

    private fun sameBoot(session: FocusSessionEntity, now: Long, boot: Int): Boolean =
        session.bootCount == boot && now >= session.checkpointElapsedRealtime

    private fun elapsed(session: FocusSessionEntity, now: Long, boot: Int): Int {
        if (session.status != FocusSessionStatus.RUNNING || !sameBoot(session, now, boot))
            return session.elapsedSeconds
        return FocusTiming.seconds(FocusTiming.elapsedMillis(
            session.elapsedSeconds, session.checkpointElapsedRealtime, now, session.durationMinutes
        ))
    }

    private fun runningCheckpoint(session: FocusSessionEntity, now: Long, boot: Int): FocusSessionEntity {
        if (!sameBoot(session, now, boot)) return session.copy(status = FocusSessionStatus.PAUSED)
        val seconds = elapsed(session, now, boot)
        return session.copy(
            elapsedSeconds = seconds,
            checkpointElapsedRealtime = FocusTiming.nextCheckpoint(
                session.checkpointElapsedRealtime, session.elapsedSeconds, seconds
            )
        )
    }

    suspend fun start(durationMinutes: Int): FocusSessionEntity = database.withTransaction {
        require(durationMinutes == 0 || durationMinutes in 5..90)
        dao.activeSession()?.let { return@withTransaction it }
        FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            createdAtMillis = clock.wallMillis(),
            durationMinutes = durationMinutes,
            checkpointElapsedRealtime = clock.monotonicMillis(),
            bootCount = clock.bootCount()
        ).also { dao.insertSession(it) }
    }

    suspend fun restore(): FocusRestore = database.withTransaction {
        val previous = dao.activeSession() ?: return@withTransaction FocusRestore(null)
        val now = clock.monotonicMillis()
        val boot = clock.bootCount()
        val differentBoot = previous.status == FocusSessionStatus.RUNNING && !sameBoot(previous, now, boot)
        val recovered = if (previous.status == FocusSessionStatus.RUNNING)
            runningCheckpoint(previous, now, boot)
        else previous
        dao.updateSession(recovered)
        FocusRestore(recovered, differentBoot)
    }

    suspend fun checkpoint(id: String): FocusSessionEntity? = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction null
        if (current.status != FocusSessionStatus.RUNNING) return@withTransaction current
        val updated = runningCheckpoint(current, clock.monotonicMillis(), clock.bootCount())
        dao.updateSession(updated)
        updated
    }

    suspend fun togglePause(id: String): FocusSessionEntity? = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction null
        val now = clock.monotonicMillis()
        val boot = clock.bootCount()
        val next = when (current.status) {
            FocusSessionStatus.RUNNING -> {
                current.copy(
                    elapsedSeconds = elapsed(current, now, boot),
                    status = FocusSessionStatus.PAUSED,
                    checkpointElapsedRealtime = now,
                    bootCount = boot
                )
            }
            FocusSessionStatus.PAUSED -> current.copy(
                status = FocusSessionStatus.RUNNING,
                checkpointElapsedRealtime = now,
                bootCount = boot
            )
            else -> return@withTransaction current
        }
        dao.updateSession(next)
        next
    }

    suspend fun finish(id: String, onlyIfDue: Boolean = false): FocusFinish? = database.withTransaction {
        val original = dao.sessionById(id) ?: return@withTransaction null
        if (original.status == FocusSessionStatus.COMPLETED || original.status == FocusSessionStatus.STOPPED)
            return@withTransaction FocusFinish(original, dao.rewardBySession(id))
        val now = clock.monotonicMillis()
        val boot = clock.bootCount()
        val seconds = elapsed(original, now, boot)
        val earned = FocusRewardRules.qualifies(original.durationMinutes, seconds)
        // An early automatic finish must never persist STOPPED or award a paused/rebooted session.
        if (onlyIfDue && (original.durationMinutes <= 0 || !earned ||
                original.status != FocusSessionStatus.RUNNING || !sameBoot(original, now, boot)))
            return@withTransaction null
        val finishedAt = clock.wallMillis()
        val result = original.copy(
            elapsedSeconds = seconds,
            finishedAtMillis = finishedAt,
            status = if (earned) FocusSessionStatus.COMPLETED else FocusSessionStatus.STOPPED
        )
        dao.updateSession(result)
        if (earned) dao.insertReward(FocusRewardEntity(id, finishedAt, 30, 20))
        FocusFinish(result, dao.rewardBySession(id))
    }

    suspend fun totalExperience(): Int = dao.totalExperience()

    suspend fun totalPoints(): Int = dao.totalPoints()

    suspend fun todayExperience(): Int {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(clock.wallMillis()).atZone(zone).toLocalDate()
        val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.experienceBetween(from, until)
    }

    suspend fun report(): FocusSummary {
        val now = Instant.ofEpochMilli(clock.wallMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
        val since = now.minusDays(29).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val completed = dao.finishedSince(since).filter { it.status == FocusSessionStatus.COMPLETED }
        val last7Start = now.minusDays(6)
        val timezone = ZoneId.systemDefault()
        fun summarize(items: List<FocusSessionEntity>): FocusReport =
            FocusReport(items.size, items.sumOf { it.elapsedSeconds }, items.size * 30)
        return FocusSummary(
            last7 = summarize(completed.filter {
                Instant.ofEpochMilli(it.finishedAtMillis ?: 0L).atZone(timezone).toLocalDate() >= last7Start
            }),
            last30 = summarize(completed)
        )
    }
}
