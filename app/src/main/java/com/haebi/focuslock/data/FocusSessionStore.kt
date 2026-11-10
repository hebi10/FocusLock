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

    private fun elapsed(session: FocusSessionEntity): Int {
        if (session.status != FocusSessionStatus.RUNNING ||
            session.bootCount != clock.bootCount() ||
            clock.monotonicMillis() < session.checkpointElapsedRealtime
        ) return session.elapsedSeconds
        val delta = ((clock.monotonicMillis() - session.checkpointElapsedRealtime) / 1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val total = (session.elapsedSeconds.toLong() + delta).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return if (session.durationMinutes > 0) total.coerceAtMost(session.durationMinutes * 60) else total
    }

    suspend fun start(durationMinutes: Int): FocusSessionEntity = database.withTransaction {
        require(durationMinutes == 0 || durationMinutes in 5..90)
        dao.activeSession()?.let {
            dao.updateSession(it.copy(
                elapsedSeconds = elapsed(it),
                status = FocusSessionStatus.STOPPED,
                finishedAtMillis = clock.wallMillis()
            ))
        }
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
        val differentBoot = previous.status == FocusSessionStatus.RUNNING &&
            (previous.bootCount != clock.bootCount() || clock.monotonicMillis() < previous.checkpointElapsedRealtime)
        val recovered = if (differentBoot) previous.copy(status = FocusSessionStatus.PAUSED)
        else if (previous.status == FocusSessionStatus.RUNNING)
            previous.copy(elapsedSeconds = elapsed(previous), checkpointElapsedRealtime = clock.monotonicMillis())
        else previous
        dao.updateSession(recovered)
        FocusRestore(recovered, differentBoot)
    }

    suspend fun checkpoint(id: String): FocusSessionEntity? = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction null
        if (current.status != FocusSessionStatus.RUNNING) return@withTransaction current
        val updated = if (current.bootCount != clock.bootCount() ||
            clock.monotonicMillis() < current.checkpointElapsedRealtime
        ) current.copy(status = FocusSessionStatus.PAUSED)
        else current.copy(elapsedSeconds = elapsed(current), checkpointElapsedRealtime = clock.monotonicMillis())
        dao.updateSession(updated)
        updated
    }

    suspend fun togglePause(id: String): FocusSessionEntity? = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction null
        val next = when (current.status) {
            FocusSessionStatus.RUNNING -> {
                val isSameBoot = current.bootCount == clock.bootCount() &&
                    clock.monotonicMillis() >= current.checkpointElapsedRealtime
                current.copy(
                    elapsedSeconds = elapsed(current),
                    status = FocusSessionStatus.PAUSED,
                    checkpointElapsedRealtime = clock.monotonicMillis(),
                    bootCount = if (isSameBoot) current.bootCount else clock.bootCount()
                )
            }
            FocusSessionStatus.PAUSED -> current.copy(
                status = FocusSessionStatus.RUNNING,
                checkpointElapsedRealtime = clock.monotonicMillis(),
                bootCount = clock.bootCount()
            )
            else -> return@withTransaction current
        }
        dao.updateSession(next)
        next
    }

    suspend fun finish(id: String): FocusFinish? = database.withTransaction {
        val original = dao.sessionById(id) ?: return@withTransaction null
        if (original.status == FocusSessionStatus.COMPLETED || original.status == FocusSessionStatus.STOPPED)
            return@withTransaction FocusFinish(original, dao.rewardBySession(id))
        val seconds = elapsed(original)
        val earned = FocusRewardRules.qualifies(original.durationMinutes, seconds)
        val result = original.copy(
            elapsedSeconds = seconds,
            finishedAtMillis = clock.wallMillis(),
            status = if (earned) FocusSessionStatus.COMPLETED else FocusSessionStatus.STOPPED
        )
        dao.updateSession(result)
        if (earned) dao.insertReward(FocusRewardEntity(id, clock.wallMillis(), 30, 20))
        FocusFinish(result, dao.rewardBySession(id))
    }

    suspend fun totalExperience(): Int = dao.totalExperience()

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
                LocalDate.ofInstant(Instant.ofEpochMilli(it.finishedAtMillis ?: 0L), timezone) >= last7Start
            }),
            last30 = summarize(completed)
        )
    }
}
