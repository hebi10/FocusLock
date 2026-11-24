package com.haebi.focuslock.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import com.haebi.focuslock.data.GoalAlerts

fun hasUsageAccess(context: Context): Boolean = try {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
        AppOpsManager.MODE_ALLOWED
} catch (_: RuntimeException) { false }

fun hasNotificationAccess(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal enum class GoalNotificationAvailability(val label: String) {
    READY("허용"), RUNTIME_DENIED("알림 권한 미허용"), APP_BLOCKED("앱 알림 차단"),
    CHANNEL_BLOCKED("목표 알림 채널 차단"), CHANNEL_MISSING("채널 생성 전"), UNKNOWN("상태 확인 불가")
}

internal fun classifyGoalNotifications(runtimeAllowed: Boolean, appAllowed: Boolean, channelImportance: Int?): GoalNotificationAvailability = when {
    !runtimeAllowed -> GoalNotificationAvailability.RUNTIME_DENIED
    !appAllowed -> GoalNotificationAvailability.APP_BLOCKED
    channelImportance == null -> GoalNotificationAvailability.CHANNEL_MISSING
    channelImportance == NotificationManager.IMPORTANCE_NONE -> GoalNotificationAvailability.CHANNEL_BLOCKED
    else -> GoalNotificationAvailability.READY
}

internal fun goalNotificationAvailability(context: Context): GoalNotificationAvailability = try {
    val manager = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    classifyGoalNotifications(hasNotificationAccess(context), manager.areNotificationsEnabled(),
        manager.getNotificationChannel(GoalAlerts.CHANNEL)?.importance)
} catch (_: RuntimeException) { GoalNotificationAvailability.UNKNOWN }

private fun openSettingsSafely(context: Context, vararg intents: Intent) {
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    Toast.makeText(context, "설정을 열 수 없습니다. Android 설정에서 직접 변경해 주세요.", Toast.LENGTH_LONG).show()
}

private fun appInfo(context: Context, packageName: String = context.packageName) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

private fun appNotifications(context: Context) = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

fun openUsageSettings(context: Context) =
    openSettingsSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), Intent(Settings.ACTION_SETTINGS))

fun openNotificationSettings(context: Context) =
    openSettingsSafely(context, appNotifications(context), appInfo(context), Intent(Settings.ACTION_SETTINGS))

internal fun openGoalChannelSettings(context: Context) =
    openSettingsSafely(context, Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, GoalAlerts.CHANNEL),
        appNotifications(context), appInfo(context), Intent(Settings.ACTION_SETTINGS))

fun openInstalledAppInfo(context: Context, packageName: String?) =
    openSettingsSafely(context, if (packageName.isNullOrBlank()) Intent(Settings.ACTION_SETTINGS)
        else appInfo(context, packageName), Intent(Settings.ACTION_SETTINGS))
