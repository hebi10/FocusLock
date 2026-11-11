package com.haebi.focuslock.data

import java.time.DayOfWeek
import java.time.LocalDate

/** ISO weekday bits: Monday is bit 0 and Sunday is bit 6. */
object GoalDayPolicy {
    const val NONE = 0
    const val EVERY_DAY = 0b1111111

    fun fromLegacyEveryday(enabled: Boolean): Int = if (enabled) EVERY_DAY else NONE

    fun normalize(mask: Int): Int = mask and EVERY_DAY

    fun selected(mask: Int, day: DayOfWeek): Boolean =
        normalize(mask) and (1 shl (day.value - 1)) != 0

    fun selected(mask: Int, date: LocalDate): Boolean = selected(mask, date.dayOfWeek)

    fun toggle(mask: Int, day: DayOfWeek): Int =
        normalize(mask) xor (1 shl (day.value - 1))
}
