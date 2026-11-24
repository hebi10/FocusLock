package com.haebi.focuslock.data

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/** Local goal settings history; no historical Android usage values are persisted. */
internal object GoalHistoryStore {
    const val TOTAL_KEY = "whole_usage_goal_minutes"
    private const val PREFIX = "goal_history_v1."

    fun read(prefs: SharedPreferences): List<GoalHistoryEvent> =
        prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(PREFIX) || value !is String) return@mapNotNull null
            val scopeAndDay = key.removePrefix(PREFIX)
            val scope = scopeAndDay.substringBeforeLast('.', "")
            val day = scopeAndDay.substringAfterLast('.').toLongOrNull() ?: return@mapNotNull null
            val values = value.split('|')
            if (scope.isEmpty() || values.size != 5) return@mapNotNull null
            val minutes = values[0].toIntOrNull()?.takeIf { it in 0..1440 } ?: return@mapNotNull null
            val days = values[1].toIntOrNull() ?: return@mapNotNull null
            val date = runCatching { LocalDate.ofEpochDay(day) }.getOrNull() ?: return@mapNotNull null
            GoalHistoryEvent(scope, date, values[4], GoalConfig(minutes, GoalDayPolicy.normalize(days),
                values[2] == "1", values[3] == "1"))
        }.sortedBy { it.date }

    fun record(
        prefs: SharedPreferences,
        editor: SharedPreferences.Editor,
        scope: String,
        config: GoalConfig,
        date: LocalDate = LocalDate.now(),
        zoneId: String = ZoneId.systemDefault().id
    ): Boolean {
        val latest = read(prefs).lastOrNull { it.scope == scope && !it.date.isAfter(date) }
        if (latest != null && latest.config == config && latest.zoneId == zoneId) return false
        editor.putString(PREFIX + scope + "." + date.toEpochDay(),
            "${config.minutes}|${config.days}|${if (config.warn80) 1 else 0}|${if (config.warn100) 1 else 0}|$zoneId")
        return true
    }

    /** Existing goals acquire a baseline today, never a fabricated past history. */
    fun seedCurrentGoals(prefs: SharedPreferences): Boolean {
        val editor = prefs.edit()
        var changed = false
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("goal.") && value is Int && value > 0) {
                val id = key.removePrefix("goal.")
                val mask = if (prefs.contains("goalDays.$id")) GoalDayPolicy.normalize(prefs.getInt("goalDays.$id", GoalDayPolicy.EVERY_DAY))
                    else GoalDayPolicy.fromLegacyEveryday(prefs.getBoolean("everyday.$id", true))
                changed = record(prefs, editor, id, GoalConfig(value, mask,
                    prefs.getBoolean("warn80.$id", true), prefs.getBoolean("warn100.$id", true))) || changed
            }
        }
        val total = prefs.getInt(TOTAL_KEY, 0)
        if (total > 0) changed = record(prefs, editor, GoalHistoryPolicy.TOTAL, GoalConfig(total)) || changed
        if (changed) editor.apply()
        return changed
    }
}
