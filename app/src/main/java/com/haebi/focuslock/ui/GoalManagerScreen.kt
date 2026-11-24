package com.haebi.focuslock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.haebi.focuslock.data.*
import java.time.LocalDate

private fun summaryData(history: List<GoalHistoryEvent>, scope: String, snapshot: UsageSnapshot?, days: Int): GoalHistorySummaryData {
    val today = snapshot?.today?.endDate ?: LocalDate.now()
    val observed = if (scope == GoalHistoryPolicy.TOTAL) snapshot?.dailyForegroundMillis.orEmpty()
        else snapshot?.appDailyForegroundMillis?.get(scope).orEmpty()
    return GoalHistoryPolicy.summarize(history, scope, today, snapshot?.observedZoneId, observed, days)
}

@Composable
internal fun GoalHistorySummary(history: List<GoalHistoryEvent>, scope: String, snapshot: UsageSnapshot?, days: Int) {
    val summary = remember(history, scope, snapshot, days) { summaryData(history, scope, snapshot, days) }
    if (summary.observed == 0) {
        Text("아직 비교 가능한 목표 이력과 사용 기록이 없습니다.", style = MaterialTheme.typography.bodySmall)
    } else {
        Text("관측 기준 목표 이하 ${summary.within}/${summary.observed}일 · ${summary.rate}%",
            style = MaterialTheme.typography.titleMedium)
        Text("연속 목표 이하 ${summary.streak}일 · 확인된 목표 초과 ${summary.over}일",
            style = MaterialTheme.typography.bodySmall)
    }
    Text("오늘은 제외합니다. 목표 변경일·이력 없음·사용 기록 미확인일은 성공으로 계산하지 않습니다. Android 기록이 완전하다는 보장은 없습니다.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun GoalManagerScreen(
    state: UsageLoadState,
    globalGoal: Int,
    globalAlerts: Boolean,
    goals: Map<String, Int>,
    history: List<GoalHistoryEvent>,
    labelFor: (String) -> String,
    onBack: () -> Unit,
    onGlobalSave: (Int, Boolean) -> Unit,
    onEditApp: (String) -> Unit,
    onAnalysis: () -> Unit
) {
    val snapshot = (state as? UsageLoadState.Ready)?.snapshot
    val apps = remember(snapshot) { listOfNotNull(snapshot?.today, snapshot?.last7, snapshot?.last30)
        .flatMap { it.apps }.distinctBy { it.id }.associateBy { it.id } }
    val active = goals.filterValues { it > 0 }
    val scopes = (listOf(GoalHistoryPolicy.TOTAL) + active.keys + history.map { it.scope }).distinct()
    fun label(id: String) = if (id == GoalHistoryPolicy.TOTAL) "전체 앱 사용 목표" else apps[id]?.name ?: labelFor(id)
    var draft by rememberSaveable(globalGoal) { mutableIntStateOf(if (globalGoal > 0) globalGoal else 180) }
    var alertDraft by rememberSaveable(globalGoal, globalAlerts) { mutableStateOf(globalAlerts) }
    var scope by rememberSaveable { mutableStateOf(GoalHistoryPolicy.TOTAL) }
    var period by rememberSaveable { mutableStateOf("7일") }
    val selected = if (scope in scopes) scope else GoalHistoryPolicy.TOTAL
    val days = if (period == "30일") 30 else 7
    val summary = remember(history, selected, snapshot, days) { summaryData(history, selected, snapshot, days) }
    AdaptiveScrollColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            Text("목표 관리", style = MaterialTheme.typography.headlineMedium)
        }
        FocusCard {
            SectionTitle("전체 앱 사용 목표")
            Text(if (globalGoal > 0) "현재 목표: " + UsageFormat.minutesLabel(globalGoal) else "설정된 전체 목표가 없습니다.")
            Text("Android 앱별 포그라운드 합계 기준입니다. 동시 사용 시 실제 경과 시간과 다를 수 있습니다.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { draft = (draft - 15).coerceAtLeast(15) }, modifier = Modifier.weight(1f)) { Text("−15분") }
                Text(UsageFormat.minutesLabel(draft), modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { draft = (draft + 15).coerceAtMost(1440) }, modifier = Modifier.weight(1f)) { Text("+15분") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("전체 목표 80%·100% 경고", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = alertDraft, onCheckedChange = { alertDraft = it })
            }
            Text("알림은 선택 사항이며 Android 권한과 절전 정책에 따라 지연될 수 있습니다.", style = MaterialTheme.typography.labelSmall)
            ActionButton("전체 목표 저장", { onGlobalSave(draft, alertDraft) }, modifier = Modifier.testTag("whole-goal-save"))
            if (globalGoal > 0) TextButton(onClick = { onGlobalSave(0, false) }) { Text("전체 목표 해제") }
        }
        FocusCard {
            SectionTitle("앱별 목표")
            if (active.isEmpty()) Text("설정한 앱별 목표가 없습니다.", style = MaterialTheme.typography.bodySmall)
            active.toSortedMap().forEach { (id, minutes) ->
                val observed = snapshot?.today?.apps?.firstOrNull { it.id == id }
                SettingRow(label(id),
                    (observed?.let { "오늘 " + UsageFormat.minutesLabel(it.minutes) } ?: "오늘 기록 미확인") +
                        " / 목표 " + UsageFormat.minutesLabel(minutes),
                    "수정") { onEditApp(id) }
            }
            TextButton(onClick = onAnalysis) { Text("사용 분석에서 앱 목표 추가") }
            Text("사용 기록 권한이 없어도 기존 목표는 수정하거나 해제할 수 있습니다.", style = MaterialTheme.typography.labelSmall)
        }
        FocusCard {
            SectionTitle("목표 이력 및 달성률")
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label(selected)) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    scopes.forEach { id -> DropdownMenuItem(text = { Text(label(id)) }, onClick = { scope = id; expanded = false }) }
                }
            }
            PeriodSelector(listOf("7일", "30일"), period) { period = it }
            GoalHistorySummary(history, selected, snapshot, days)
            summary.days.asReversed().forEach { day ->
                val status = when (day.result) {
                    GoalDayResult.WITHIN -> "관측 목표 이하"
                    GoalDayResult.OVER -> "목표 초과"
                    GoalDayResult.CHANGED -> "목표 변경일 · 평가 제외"
                    GoalDayResult.UNKNOWN -> "기록 미확인"
                    GoalDayResult.NO_GOAL -> "목표 이력 없음"
                }
                Text("${day.date.monthValue}월 ${day.date.dayOfMonth}일 · $status",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (day.result == GoalDayResult.OVER) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                if (day.result == GoalDayResult.WITHIN || day.result == GoalDayResult.OVER) {
                    Text("관측 " + UsageFormat.minutesLabel(UsageMetrics.roundedMinutes(day.millis!!)) +
                        " / 목표 " + UsageFormat.minutesLabel(day.limit!!),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
