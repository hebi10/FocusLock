package com.haebi.focuslock.data

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/** Local goal settings history; no historical Android usage values are persisted. */
internal object GoalHistoryStore {
    const val TOTAL_KEY = "whole_usage_goal_minutes"
    const val TOTAL_ALERT_KEY = "whole_usage_goal_alerts"
    private const val PREFIX = "goal_history_v1."
    private const val LABEL_PREFIX = "goal_label."

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

    internal fun currentGoals(prefs: SharedPreferences): Map<String, GoalConfig> = buildMap {
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("goal.") && value is Int && value > 0) {
                val id = key.removePrefix("goal.")
                val mask = if (prefs.contains("goalDays.$id")) GoalDayPolicy.normalize(prefs.getInt("goalDays.$id", GoalDayPolicy.EVERY_DAY))
                    else GoalDayPolicy.fromLegacyEveryday(prefs.getBoolean("everyday.$id", true))
                put(id, GoalConfig(value, mask, prefs.getBoolean("warn80.$id", true),
                    prefs.getBoolean("warn100.$id", true)))
            }
        }
        val total = prefs.getInt(TOTAL_KEY, 0)
        if (total > 0) {
            val alerts = prefs.getBoolean(TOTAL_ALERT_KEY, false)
            put(GoalHistoryPolicy.TOTAL, GoalConfig(total, warn80 = alerts, warn100 = alerts))
        }
    }

    private fun putEvent(editor: SharedPreferences.Editor, scope: String, config: GoalConfig, date: LocalDate, zoneId: String) {
        editor.putString(PREFIX + scope + "." + date.toEpochDay(),
            "${config.minutes}|${config.days}|${if (config.warn80) 1 else 0}|${if (config.warn100) 1 else 0}|$zoneId")
    }

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
        putEvent(editor, scope, config, date, zoneId)
        return true
    }

    /** Scan history once rather than once per configured app. */
    @Synchronized
    fun seedCurrentGoals(prefs: SharedPreferences): Boolean {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.now(zone)
        val latest = read(prefs).filter { !it.date.isAfter(date) }.associateBy { it.scope }
        val editor = prefs.edit()
        var changed = false
        currentGoals(prefs).forEach { (scope, config) ->
            val previous = latest[scope]
            if (previous == null || previous.config != config || previous.zoneId != zone.id) {
                putEvent(editor, scope, config, date, zone.id)
                changed = true
            }
        }
        if (changed) editor.apply()
        return changed
    }

    /** Clear past goal history only; preserve current goals, alert markers and all other settings. */
    @Synchronized
    fun clearHistory(prefs: SharedPreferences, date: LocalDate = LocalDate.now(), zoneId: String = ZoneId.systemDefault().id): Boolean {
        val current = currentGoals(prefs)
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(PREFIX) ||
                (key.startsWith(LABEL_PREFIX) && key.removePrefix(LABEL_PREFIX) !in current)) editor.remove(key)
        }
        current.forEach { (scope, config) -> putEvent(editor, scope, config, date, zoneId) }
        return editor.commit()
    }
}
