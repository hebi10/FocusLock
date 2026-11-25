package com.haebi.focuslock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.platform.LocalDensity
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
    onRefresh: () -> Unit,
    globalGoal: Int = 0,
    onGoals: () -> Unit = {}
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
    val goalCount = goals.values.count { it > 0 }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageTitle("오늘의 FocusLock", LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))) {
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "설정") }
        }

        if (snapshot == null) {
            UsageStatus(state, onUsageSettings, onRefresh)
        } else {
            FocusCard(contentPadding = 12.dp, contentSpacing = 6.dp) {
                Metric(if (snapshot.today.apps.isEmpty()) "기록 미확인" else UsageFormat.minutesLabel(snapshot.today.totalMinutes), "오늘 사용 시간")
                if (snapshot.today.apps.isEmpty()) {
                    Text(
                        "아직 확인된 앱 사용 기록이 없습니다. 다른 앱을 사용한 뒤 다시 확인해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (snapshot.today.apps.isNotEmpty()) {
                    val hours = AnalysisModels.bars(snapshot, "오늘")
                    if (hours != null && hours.any { it.observed }) {
                        ReferenceUsageSparkline(hours, Modifier.fillMaxWidth().height(52.dp))
                    }
                }
                ReferenceMeasurementNote(
                    summary = "오늘 진행 중 · Android 관측 합계",
                    detail = UsageComparisonPolicy.summary(snapshot, "오늘") + "\nAndroid에서 제공한 기기 사용 통계입니다.\n" + UsageComparisonPolicy.foregroundNotice,
                    tag = "reference-home-note"
                )
                TextButton(onClick = onAnalysis) { Text("사용 현황 자세히 보기") }
            }

            if (globalGoal > 0) {
                HomeGoalCard(snapshot, globalGoal, goalCount, topApp, onGoals, onSetGoal)
            }
            if (snapshot.today.apps.isNotEmpty()) {
                FocusCard(contentPadding = 12.dp, contentSpacing = 4.dp) {
                    SectionTitle("많이 사용한 앱", "전체 보기", onAnalysis)
                    if (visibleApps.isEmpty()) {
                        Text(
                            "표시할 앱이 없습니다. 제외 설정은 사용 분석에서 확인할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    visibleApps.take(3).forEachIndexed { index, app ->
                        ReferenceRankRow(app, snapshot.today.totalMinutes) { onApp(app) }
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
                FocusCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
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

        if (snapshot == null || globalGoal <= 0) {
            HomeGoalCard(snapshot, globalGoal, goalCount, topApp, onGoals, onSetGoal)
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
                    if (snapshot.last7.apps.isEmpty()) "기록 미확인" else UsageFormat.minutesLabel(snapshot.last7.totalMinutes),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    UsageComparisonPolicy.summary(snapshot, "7일") + "\n" + UsageComparisonPolicy.partialTodayNotice,
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
private fun HomeGoalCard(
    snapshot: UsageSnapshot?, globalGoal: Int, goalCount: Int,
    topApp: AppUsage?, onGoals: () -> Unit, onSetGoal: (AppUsage) -> Unit
) {
    FocusCard(contentPadding = 12.dp, contentSpacing = 6.dp) {
        SectionTitle("사용 목표", "목표 관리", onGoals)
        if (globalGoal > 0) {
            Text("전체 앱 사용 목표 · " + UsageFormat.minutesLabel(globalGoal),
                style = MaterialTheme.typography.titleMedium)
            val millis = snapshot?.today?.totalForegroundMillis?.takeIf { it > 0 }
            if (millis == null) Text("오늘 사용 기록 미확인", style = MaterialTheme.typography.bodySmall)
            else {
                val limit = globalGoal.toLong() * 60_000L
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${(millis.toDouble() / limit * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, color = if (millis > limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                    Text(UsageFormat.minutesLabel(snapshot!!.today.totalMinutes) + " / " + UsageFormat.minutesLabel(globalGoal), style = MaterialTheme.typography.bodySmall)
                }
                ProgressBar((millis.toDouble() / limit).toFloat(), isWarning = millis > limit)
                Text(if (millis > limit) "관측 사용량이 전체 목표를 초과했습니다." else "오늘은 진행 중입니다.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else Text("하루 전체 앱 사용 목표를 선택해서 설정할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall)
        Text("설정된 앱별 목표 ${goalCount}개", style = MaterialTheme.typography.bodyMedium)
        Text("개별 앱 목표의 합계와 전체 앱 사용 목표는 별개입니다.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (goalCount == 0 && topApp != null) {
            TextButton(onClick = { onSetGoal(topApp) }) { Text("앱별 사용 목표 설정") }
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
        val wide = maxWidth >= 700.dp && maxWidth / LocalDensity.current.fontScale >= 600.dp
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

                if (selected.apps.isEmpty()) {
                    InfoState("사용 기록 없음", "선택한 기간에 Android가 제공하는 앱 사용 기록이 없습니다.", "새로고침", onRefresh)
                } else {
                    FocusCard(contentPadding = 12.dp, contentSpacing = 6.dp) {
                        Metric(UsageFormat.minutesLabel(selected.totalMinutes), "총 사용 시간")
                        ReferenceMeasurementNote(
                            summary = if (period == "오늘") "오늘 진행 중 · 어제와 직접 비교하지 않음" else UsageComparisonPolicy.summary(snapshot, period),
                            detail = if (period == "오늘") UsageComparisonPolicy.foregroundNotice else UsageComparisonPolicy.partialTodayNotice + "\n" + UsageComparisonPolicy.foregroundNotice,
                            tag = "reference-analysis-note"
                        )
                    }
                    val observedInsights = UsageInsightPolicy.forPeriod(snapshot, period, excluded)
                    if (query.isBlank() && !wide) {
                        AnalysisChart(snapshot, period)
                        AnalysisCategorySummary(selected)
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
                        AnalysisAppList(selected, period, excluded, category, sort, query,
                            onCategory = { category = it }, onSort = { sort = it },
                            onQuery = { query = it }, onApp = onApp, onReport = onReport)
                        if (query.isNotBlank()) AnalysisCategorySummary(selected)
                    }
                    FocusCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
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
                }
            }
        }
    }
}
