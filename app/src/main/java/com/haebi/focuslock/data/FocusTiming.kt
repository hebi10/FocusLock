package com.haebi.focuslock.data

/** One time calculation shared by the persisted checkpoint and the displayed timer. */
internal object FocusTiming {
    private const val SECOND = 1000L

    fun elapsedMillis(
        savedSeconds: Int,
        checkpointMillis: Long,
        nowMillis: Long,
        durationMinutes: Int
    ): Long {
        val maximum = if (durationMinutes > 0) durationMinutes.toLong() * 60 * SECOND
            else Int.MAX_VALUE.toLong() * SECOND
        val saved = (savedSeconds.coerceAtLeast(0).toLong() * SECOND).coerceAtMost(maximum)
        val delta = (nowMillis - checkpointMillis).coerceIn(0L, maximum - saved)
        return saved + delta
    }

    fun seconds(millis: Long): Int = (millis.coerceAtLeast(0L) / SECOND)
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    // Advance by whole counted seconds, not wall clock now: preserve the uncounted fraction.
    fun nextCheckpoint(oldCheckpoint: Long, oldSeconds: Int, newSeconds: Int): Long =
        oldCheckpoint + (newSeconds.toLong() - oldSeconds).coerceAtLeast(0L) * SECOND

    fun remainingSeconds(durationMinutes: Int, millis: Long): Int =
        if (durationMinutes <= 0) 0 else (durationMinutes * 60 - seconds(millis)).coerceAtLeast(0)

    fun nextTickDelay(millis: Long): Long = SECOND - millis.coerceAtLeast(0L) % SECOND
}
