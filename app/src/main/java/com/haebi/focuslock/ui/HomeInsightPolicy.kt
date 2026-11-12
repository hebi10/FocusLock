package com.haebi.focuslock.ui

internal object HomeInsightPolicy {
    /** UsageEvents-derived peak hour is optional and must never be invented. */
    fun observedPeakHour(hourlyMinutes: List<Int>?): Int? {
        if (hourlyMinutes == null || hourlyMinutes.size != 24) return null
        val maximum = hourlyMinutes.maxOrNull() ?: return null
        if (maximum <= 0) return null
        return hourlyMinutes.indexOf(maximum).takeIf { it in 0..23 }
    }

    /** Shares are omitted when Android supplies no valid daily total. */
    fun topAppShare(topMinutes: Int, totalMinutes: Int): Int? {
        if (totalMinutes <= 0 || topMinutes <= 0 || topMinutes > totalMinutes) return null
        return ((topMinutes.toDouble() / totalMinutes) * 100).toInt().coerceIn(0, 100)
    }
}
