package com.haebi.focuslock.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.haebi.focuslock.MainActivity

/** Unique immutable notification intents; no external intent filter is registered. */
internal object GoalAlertNavigation {
    private const val ACTION = "com.haebi.focuslock.action.OPEN_GOAL"
    private const val SCHEME = "focuslock"
    private const val HOST = "goal"

    fun intent(context: Context, scope: String): Intent = Intent(context, MainActivity::class.java)
        .setAction(ACTION)
        .setData(Uri.Builder().scheme(SCHEME).authority(HOST).appendPath(scope).build())
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun pendingIntent(context: Context, scope: String): PendingIntent =
        PendingIntent.getActivity(context, 0, intent(context, scope),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun target(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (intent.action != ACTION || data.scheme != SCHEME || data.authority != HOST ||
            data.pathSegments.size != 1 || data.query != null || data.fragment != null) return null
        return data.pathSegments.single().takeIf { scope ->
            scope == GoalHistoryPolicy.TOTAL ||
                (scope.length <= 255 && scope.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")))
        }
    }
}
