package com.haebi.focuslock.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId

/** Export only app-owned records, never Android's usage history or arbitrary preference keys. */
internal class LocalDataStore(
    private val prefs: SharedPreferences,
    private val focusStore: FocusSessionStore
) {
    private fun configJson(config: GoalConfig) = JSONObject()
        .put("minutes", config.minutes)
        .put("alertDaysMask", config.days)
        .put("warn80", config.warn80)
        .put("warn100", config.warn100)

    suspend fun exportJson(): String {
        val goals = JSONArray()
        GoalHistoryStore.currentGoals(prefs).toSortedMap().forEach { (scope, config) ->
            goals.put(configJson(config).put("scope", scope)
                .put("label", if (scope == GoalHistoryPolicy.TOTAL) "전체 앱 사용 목표"
                    else prefs.getString("goal_label.$scope", scope) ?: scope))
        }
        val history = JSONArray()
        GoalHistoryStore.read(prefs).forEach { event ->
            history.put(configJson(event.config).put("scope", event.scope)
                .put("date", event.date.toString()).put("zoneId", event.zoneId))
        }
        val focus = focusStore.historySnapshot()
        val rewards = focus.rewards.associateBy { it.sessionId }
        val sessions = JSONArray()
        focus.sessions.forEach { session ->
            val reward = rewards[session.id]
            sessions.put(JSONObject().put("sessionId", session.id)
                .put("createdAtMillis", session.createdAtMillis)
                .put("finishedAtMillis", session.finishedAtMillis ?: JSONObject.NULL)
                .put("durationMinutes", session.durationMinutes)
                .put("elapsedSeconds", session.elapsedSeconds)
                .put("status", session.status)
                .put("reward", reward?.let { JSONObject().put("earnedAtMillis", it.earnedAtMillis)
                    .put("experience", it.experience).put("points", it.points) } ?: JSONObject.NULL))
        }
        return JSONObject().put("format", "FocusLock-local-records")
            .put("schemaVersion", 1)
            .put("exportedAtMillis", System.currentTimeMillis())
            .put("zoneId", ZoneId.systemDefault().id)
            .put("goals", goals).put("goalHistory", history).put("focusHistory", sessions)
            .put("gardenItem", prefs.getString("placed_garden_item_id", null)
                ?: prefs.getString("placed_garden_item", null) ?: "sprout")
            .put("includesAndroidUsageHistory", false)
            .put("includesActiveFocusSession", false)
            .toString(2)
    }

    fun clearGoalHistory(): Boolean = GoalHistoryStore.clearHistory(prefs)

    suspend fun clearFocusHistory(): Int? = focusStore.deleteFinalizedHistory()
}
