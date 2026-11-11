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
    onFocus: () -> Unit,
    onAnalysis: () -> Unit,
    onApp: (AppUsage) -> Unit,
    onGarden: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
    onUsageSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val snapshot = (state as? UsageLoadState.Ready)?.snapshot
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("오늘의 FocusLock", LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))) {
            TextButton(onClick = onSettings) { Text("설정") }
        }
        if (state !is UsageLoadState.Ready || snapshot?.today?.apps?.isEmpty() == true) UsageStatus(state, onUsageSettings, onRefresh)
        if (snapshot != null && snapshot.today.apps.isNotEmpty()) {
            val total = snapshot.today.totalMinutes
            FocusCard {
                Metric(UsageFormat.minutesLabel(total), "오늘 사용 시간")
                Text(UsageFormat.comparison(total, snapshot.yesterday.totalMinutes) ?: "어제 비교 기록 없음", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val totalGoals = goals.values.sum()
                if (totalGoals > 0) {
                    Text("설정된 앱 목표 합계 ${UsageFormat.minutesLabel(totalGoals)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val managedMinutes = snapshot.today.apps.filter { goals.containsKey(it.id) }.sumOf { it.minutes }
                    Text("관리 대상 앱 ${UsageFormat.minutesLabel(managedMinutes)} 사용", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProgressBar(managedMinutes.toFloat() / totalGoals)
                } else Text("앱별 목표를 설정하면 진행률을 확인할 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ActionButton("집중 시작", onFocus)
                Text("25분 집중", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            FocusCard {
                SectionTitle("많이 사용한 앱", "전체 보기", onAnalysis)
                snapshot.today.apps.filter { excluded[it.id] != true }.take(3).forEachIndexed { index, app ->
                    AppUsageRow(app, goals[app.id], snapshot.today.totalMinutes) { onApp(app) }
                    if (index < minOf(3, snapshot.today.apps.count { excluded[it.id] != true }) - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        } else {
            ActionButton("집중 시작", onFocus)
        }
        FocusCard {
            SectionTitle("나의 정원", "정원 열기", onGarden)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.garden_illustration), "정원 캐릭터와 새싹", modifier = Modifier.weight(1f).height(95.dp))
                Column(Modifier.weight(1f)) {
                    Text("나의 작은 정원", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("집중을 마치면 정원이 성장해요", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        if (snapshot != null) FocusCard {
            SectionTitle("지난 7일 변화", "리포트 보기", onReport)
            Text(UsageFormat.comparison(snapshot.last7.totalMinutes, snapshot.previous7.totalMinutes) ?: "비교할 이전 주 기록이 없습니다.", fontSize = 14.sp)
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
                        AnalysisChart(snapshot, period)
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
