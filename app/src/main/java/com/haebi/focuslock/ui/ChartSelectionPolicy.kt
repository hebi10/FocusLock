package com.haebi.focuslock.ui

internal object ChartSelectionPolicy {
    fun index(count: Int, requested: Int): Int =
        if (count <= 0) -1 else requested.coerceIn(0, count - 1)

    fun step(count: Int, current: Int, delta: Int): Int {
        if (count <= 0) return -1
        return (index(count, current).toLong() + delta).coerceIn(0L, count.toLong() - 1).toInt()
    }
}
