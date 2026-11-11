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
            val totalGoals = goals.values.filter { it > 0 }.sum()
            val managedMinutes = snapshot.today.apps
                .filter { (goals[it.id] ?: 0) > 0 }
                .sumOf { it.minutes }
            FocusCard {
                Metric(UsageFormat.minutesLabel(snapshot.today.totalMinutes), "오늘 사용 시간")
                if (snapshot.today.apps.isEmpty()) {
                    Text(
                        "오늘 확인된 앱 사용 기록이 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        UsageFormat.comparison(snapshot.today.totalMinutes, snapshot.yesterday.totalMinutes)
                            ?: "어제 비교 기록 없음",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (totalGoals > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("관리 앱 목표 진행률", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            UsageFormat.minutesLabel(managedMinutes) + " / " + UsageFormat.minutesLabel(totalGoals),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    ProgressBar(managedMinutes.toFloat() / totalGoals, isWarning = managedMinutes > totalGoals)
                    Text(
                        if (managedMinutes > totalGoals) "합산 목표보다 " + UsageFormat.minutesLabel(managedMinutes - totalGoals) + " 초과"
                        else "합산 목표까지 " + UsageFormat.minutesLabel(totalGoals - managedMinutes) + " 남음",
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
                        "설정한 앱별 사용 목표가 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val firstApp = visibleApps.firstOrNull()
                    if (firstApp != null) {
                        TextButton(onClick = { onSetGoal(firstApp) }) {
                            Text("가장 많이 사용한 앱 목표 설정")
                        }
                    } else {
                        TextButton(onClick = onAnalysis) { Text("사용 분석에서 앱 확인") }
                    }
                }
            }
        }

        ActionButton("집중 시작", onFocus)

        if (snapshot != null) {
            if (snapshot.today.apps.isEmpty()) {
                InfoState(
                    "사용 기록 확인",
                    "Android에 오늘 사용량이 기록되지 않았습니다. 다른 앱을 이용한 뒤 새로고침해 주세요.",
                    "새로고침",
                    onRefresh
                )
            } else {
                FocusCard {
                    SectionTitle("많이 사용한 앱", "전체 보기", onAnalysis)
                    if (visibleApps.isEmpty()) {
                        Text(
                            "분석에 포함된 앱이 없습니다.",
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
        }

        FocusCard {
            SectionTitle("나의 정원", "정원 열기", onGarden)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painterResource(R.drawable.garden_illustration),
                    "정원 캐릭터와 새싹",
                    modifier = Modifier.weight(1f).height(95.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("오늘 획득 " + todayExperience + " XP", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "누적 " + totalExperience + " XP · 집중 완료 보상",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                        ?: "이전 7일 비교 기록 없음",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    var filterMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val snapshot = (state as? UsageLoadState.Ready)?.snapshot
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle("사용 분석") { TextButton(onClick = onRefresh) { Text("새로고침") } }
            PeriodSelector(listOf("오늘", "7일", "30일"), period) { period = it }
            if (snapshot == null) {
                UsageStatus(state, onUsageSettings, onRefresh)
            } else {
                val selected = snapshot.window(period)
                val prior = snapshot.prior(period)
                val filtered = selected.apps.filter { excluded[it.id] != true && (category == "전체" || it.category == category) }
                    .let { if (sort == "실행 횟수") it.sortedByDescending { app -> app.launches } else it.sortedByDescending { app -> app.minutes } }
                if (selected.apps.isEmpty()) InfoState("사용 기록 없음", "선택한 기간에 Android가 제공하는 앱 사용 기록이 없습니다.")
                FocusCard {
                    Metric(UsageFormat.minutesLabel(selected.totalMinutes), "총 사용 시간")
                    Text(UsageFormat.comparison(selected.totalMinutes, prior.totalMinutes) ?: "이전 기간의 비교 기록 없음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val chartContent: @Composable () -> Unit = {
                    FocusCard {
                        SectionTitle(if(period == "오늘") "시간대별 사용량" else "일별 사용 시간")
                        val vals = snapshot.chart(period)
                        if (vals == null) Text("시간대별 사용 이벤트를 확인할 수 없습니다. 일일 사용 시간은 위 집계를 참고해 주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        else {
                            val labels = when(period) {
                                "오늘" -> List(24) { if(it % 3 == 0) "${it}시" else "" }
                                "7일" -> (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()).format(DateTimeFormatter.ofPattern("E", Locale.KOREAN)) }
                                else -> (29 downTo 0).map { if (it % 5 == 0) LocalDate.now().minusDays(it.toLong()).dayOfMonth.toString() else "" }
                            }
                            BarChart(vals, labels, (vals.maxOrNull() ?: 0).coerceAtLeast(1), -1)
                            if (period == "오늘") Text("시간대별 수치는 보관된 앱 전환 이벤트를 기반으로 하며 일부 누락될 수 있습니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val listContent: @Composable () -> Unit = {
                    FocusCard {
                        SectionTitle("카테고리별 사용 비중")
                        selected.apps.groupBy { it.category }.toList().sortedByDescending { (_, list) -> list.sumOf { it.minutes } }
                            .forEach { (cat, items) -> CategoryRow(cat, items.sumOf { it.minutes }, selected.totalMinutes) }
                        if (selected.apps.isEmpty()) Text("분류할 기록 없음", fontSize = 12.sp)
                    }
                    FocusCard {
                        SectionTitle("앱별 사용 시간", "리포트 보기", onReport)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box {
                                OutlinedButton(onClick = { filterMenu = true }) { Text(category) }
                                DropdownMenu(filterMenu, onDismissRequest = { filterMenu = false }) {
                                    (listOf("전체") + selected.apps.map { it.category }.distinct()).forEach { label -> DropdownMenuItem(text = { Text(label) }, onClick = { category = label; filterMenu = false }) }
                                }
                            }
                            Box {
                                OutlinedButton(onClick = { sortMenu = true }) { Text(sort) }
                                DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                                    listOf("사용 시간", "실행 횟수").forEach { label -> DropdownMenuItem(text = { Text(label) }, onClick = { sort = label; sortMenu = false }) }
                                }
                            }
                        }
                        if (filtered.isEmpty()) Text("선택한 조건의 사용 기록이 없습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        filtered.forEachIndexed { index, app ->
                            AppUsageRow(app, totalMinutes = selected.totalMinutes, onClick = { onApp(app, period) })
                            if (index < filtered.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { chartContent() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { listContent() }
                } else { chartContent(); listContent() }
            }
        }
    }
}
