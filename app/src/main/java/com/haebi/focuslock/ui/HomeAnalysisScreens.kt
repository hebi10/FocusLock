package com.haebi.focuslock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.R
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.UsageWindow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun UsageStatus(state: UsageLoadState, onGrant: () -> Unit, onRefresh: () -> Unit) {
    when (state) {
        UsageLoadState.Loading -> InfoState("사용 기록 확인 중", "기기에 저장된 Android 사용 기록을 읽고 있습니다.")
        UsageLoadState.PermissionRequired -> InfoState("사용 정보 접근 필요", "앱별 실제 사용 시간을 확인하려면 Android 설정에서 FocusLock의 사용 정보 접근을 허용해 주세요. 집중 타이머는 권한 없이도 이용할 수 있습니다.", "권한 설정 열기", onGrant)
        is UsageLoadState.Unavailable -> InfoState("사용 기록을 불러오지 못했습니다", state.reason, "다시 시도", onRefresh)
        is UsageLoadState.Ready -> if (state.snapshot.today.apps.isEmpty()) InfoState("아직 사용 기록이 없습니다", "Android에서 확인된 사용 시간이 없습니다. 다른 앱을 사용한 뒤 다시 확인해 주세요.", "새로고침", onRefresh)
    }
}

@Composable
fun HomeScreen(
    state: UsageLoadState,
    goals: Map<String, Int>,
    excluded: Map<String, Boolean>,
    todayExperience: Int,
    totalExperience: Int,
    onFocus: () -> Unit,
    onAnalysis: () -> Unit,
    onApp: (AppUsage) -> Unit,
    onSetGoal: (AppUsage) -> Unit,
    onGarden: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
    onUsageSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val snapshot = (state as? UsageLoadState.Ready)?.snapshot
    val visibleApps = snapshot?.today?.apps.orEmpty().filter { excluded[it.id] != true }
    val topApp = visibleApps.firstOrNull { it.minutes > 0 }
    val todayInsights = snapshot?.let {
        UsageInsightPolicy.forPeriod(it, "오늘", excluded)
    }.orEmpty()
    val weeklyInsights = snapshot?.let {
        UsageInsightPolicy.weeklyChanges(it, excluded)
    }.orEmpty()
    val goalSuggestion = snapshot?.let {
        UsageInsightPolicy.goalSuggestion(it, goals, excluded)
    }
    val totalGoals = goals.values.filter { it > 0 }.sum()
    val managedMinutes = snapshot?.today?.apps.orEmpty()
        .filter { (goals[it.id] ?: 0) > 0 }
        .sumOf { it.minutes }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle("오늘의 FocusLock", LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))) {
            TextButton(onClick = onSettings) { Text("설정") }
        }

        if (snapshot == null) {
            UsageStatus(state, onUsageSettings, onRefresh)
        } else {
            FocusCard {
                Metric(UsageFormat.minutesLabel(snapshot.today.totalMinutes), "오늘 사용 시간")
                if (snapshot.today.apps.isEmpty()) {
                    Text(
                        "아직 확인된 앱 사용 기록이 없습니다. 다른 앱을 사용한 뒤 다시 확인해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        UsageFormat.comparison(snapshot.today.totalMinutes, snapshot.yesterday.totalMinutes)
                            ?.replace("이전 기간", "어제") ?: "어제와 비교할 사용 기록이 부족합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Android에서 제공한 기기 사용 통계입니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onAnalysis) { Text("사용 현황 자세히 보기") }
            }

            if (snapshot.today.apps.isNotEmpty()) {
                FocusCard {
                    SectionTitle("많이 사용한 앱", "전체 보기", onAnalysis)
                    if (visibleApps.isEmpty()) {
                        Text(
                            "표시할 앱이 없습니다. 제외 설정은 사용 분석에서 확인할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    visibleApps.take(3).forEachIndexed { index, app ->
                        AppUsageRow(app, goals[app.id], snapshot.today.totalMinutes) { onApp(app) }
                        val goal = goals[app.id]
                        if (goal != null && goal > 0) {
                            Column(
                                Modifier.fillMaxWidth().padding(start = 50.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "목표 " + UsageFormat.minutesLabel(goal),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        if (app.minutes > goal) UsageFormat.minutesLabel(app.minutes - goal) + " 초과"
                                        else UsageFormat.minutesLabel(goal - app.minutes) + " 남음",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (app.minutes > goal) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ProgressBar(app.minutes.toFloat() / goal, isWarning = app.minutes > goal)
                            }
                        }
                        if (index < minOf(3, visibleApps.size) - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            if (todayInsights.isNotEmpty() || weeklyInsights.isNotEmpty() || goalSuggestion != null) {
                FocusCard {
                    SectionTitle("관측 기반 사용 패턴")
                    (todayInsights + weeklyInsights.take(1)).forEach { insight ->
                        Text(insight.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            insight.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    goalSuggestion?.let { suggestion ->
                        Text(suggestion.insight.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            suggestion.insight.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onSetGoal(suggestion.app) }) {
                            Text("사용 목표 설정")
                        }
                    }
                }
            }
        }

        if (snapshot != null) {
            FocusCard {
                SectionTitle("사용 목표")
                if (totalGoals > 0) {
                    Text(
                        "설정한 앱별 목표의 합계 " + UsageFormat.minutesLabel(totalGoals),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        UsageFormat.minutesLabel(managedMinutes) + " / " +
                            UsageFormat.minutesLabel(totalGoals),
                        style = MaterialTheme.typography.labelMedium
                    )
                    ProgressBar(
                        managedMinutes.toFloat() / totalGoals,
                        isWarning = managedMinutes > totalGoals
                    )
                    Text(
                        if (managedMinutes > totalGoals)
                            "목표를 설정한 앱의 합산 사용 시간이 " +
                                UsageFormat.minutesLabel(managedMinutes - totalGoals) + " 초과했습니다."
                        else "설정된 앱 목표 합계까지 " +
                            UsageFormat.minutesLabel(totalGoals - managedMinutes) + " 남았습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (managedMinutes > totalGoals) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val editable = listOf(snapshot.today, snapshot.last7, snapshot.last30)
                        .asSequence().flatMap { it.apps.asSequence() }
                        .firstOrNull { (goals[it.id] ?: 0) > 0 }
                    if (editable != null) {
                        TextButton(onClick = { onSetGoal(editable) }) { Text("앱 목표 수정") }
                    }
                } else {
                    Text(
                        "사용 목표는 선택 사항입니다. 필요할 때 앱별로 설정할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (topApp != null) {
                        TextButton(onClick = { onSetGoal(topApp) }) { Text("앱별 사용 목표 설정") }
                    } else {
                        TextButton(onClick = onAnalysis) { Text("사용 분석에서 앱 확인") }
                    }
                }
            }
        }

        FocusCard {
            SectionTitle("필요할 때 집중하기")
            Text(
                "집중이 필요하면 10분·25분·자유 집중 타이머를 선택할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionButton("집중 시작", onFocus)
        }

        if (snapshot != null) {
            FocusCard {
                SectionTitle("지난 7일 변화", "리포트 보기", onReport)
                Text(
                    UsageFormat.minutesLabel(snapshot.last7.totalMinutes),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    UsageFormat.comparison(snapshot.last7.totalMinutes, snapshot.previous7.totalMinutes)
                        ?.replace("이전 기간", "이전 7일") ?: "이전 7일과 비교할 기록이 부족합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FocusCard {
            SectionTitle("정원·집중 성과", "정원 열기", onGarden)
            Text(
                "오늘 획득 " + todayExperience + " XP · 누적 " + totalExperience + " XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalysisScreen(
    state: UsageLoadState,
    excluded: Map<String, Boolean>,
    onApp: (AppUsage, String) -> Unit,
    onReport: () -> Unit,
    onUsageSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    var period by rememberSaveable { mutableStateOf("오늘") }
    var category by rememberSaveable { mutableStateOf("전체") }
    var sort by rememberSaveable { mutableStateOf("사용 시간") }
    var query by rememberSaveable { mutableStateOf("") }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
        ) {
            PageTitle("사용 분석") {
                TextButton(onClick = onRefresh) { Text("새로고침") }
            }
            PeriodSelector(AnalysisModels.periods, period) {
                period = it
                category = "전체"
            }

            if (state !is UsageLoadState.Ready) {
                UsageStatus(state, onUsageSettings, onRefresh)
            } else {
                val snapshot = state.snapshot
                val selected = snapshot.window(period)
                val prior = snapshot.prior(period)

                if (selected.apps.isEmpty()) {
                    InfoState("사용 기록 없음", "선택한 기간에 Android가 제공하는 앱 사용 기록이 없습니다.", "새로고침", onRefresh)
                } else {
                    FocusCard {
                        Metric(UsageFormat.minutesLabel(selected.totalMinutes), "총 사용 시간")
                        Text(
                            UsageFormat.comparison(selected.totalMinutes, prior.totalMinutes)
                                ?: "이전 기간의 비교 기록 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val observedInsights = UsageInsightPolicy.forPeriod(snapshot, period, excluded)
                    FocusCard {
                        SectionTitle("기록 기반 인사이트")
                        if (observedInsights.isEmpty()) {
                            Text(
                                "이 기간에는 비교 가능한 사용 기록이나 관측 이벤트가 부족합니다. 기록이 없는 날을 0분으로 추정하지 않습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            observedInsights.forEach { insight ->
                                Text(insight.message, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    insight.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (wide) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FocusSpacing.md)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)) {
                                AnalysisChart(snapshot, period)
                                AnalysisCategorySummary(selected)
                            }
                            Box(Modifier.weight(1f)) {
                                AnalysisAppList(selected, period, excluded, category, sort, query,
                                    onCategory = { category = it }, onSort = { sort = it },
                                    onQuery = { query = it }, onApp = onApp, onReport = onReport)
                            }
                        }
                    } else {
                        if (query.isBlank()) AnalysisChart(snapshot, period)
                        AnalysisAppList(selected, period, excluded, category, sort, query,
                            onCategory = { category = it }, onSort = { sort = it },
                            onQuery = { query = it }, onApp = onApp, onReport = onReport)
                        AnalysisCategorySummary(selected)
                    }
                }
            }
        }
    }
}
