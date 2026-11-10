package com.haebi.focuslock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.haebi.focuslock.data.DemoData

@Composable
fun HomeScreen(
    goals: Map<String, Int>,
    onFocus: () -> Unit,
    onAnalysis: () -> Unit,
    onApp: (AppUsage) -> Unit,
    onGarden: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageTitle("오늘의 FocusLock", "11월 10일 화요일") {
                TextButton(onClick = onSettings) { Text("설정", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item { DemoNotice() }
        item {
            FocusCard {
                Text("오늘 사용 시간", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Metric(DemoData.minutesLabel(DemoData.todayTotalMinutes), "전체 앱 사용", "어제보다 18분 감소")
                Text("목표 3시간 중 ${DemoData.minutesLabel(DemoData.todayTotalMinutes)} 사용", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                ProgressBar(DemoData.todayTotalMinutes.toFloat() / DemoData.dailyGoalMinutes)
                Spacer(Modifier.height(8.dp))
                ActionButton("집중 시작", onFocus)
                Text("25분 집중 · 방해 앱 3개", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        item {
            FocusCard {
                SectionTitle("많이 사용한 앱", "전체 보기", onAnalysis)
                DemoData.apps.take(3).forEachIndexed { index, app ->
                    AppUsageRow(app, goals[app.id]) { onApp(app) }
                    if (index != 2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item {
            FocusCard {
                SectionTitle("나의 정원", "정원 열기", onGarden)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painterResource(R.drawable.garden_illustration), "정원 캐릭터와 새싹",
                        modifier = Modifier.weight(1f).height(95.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("오늘 경험치 +30", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("식물이 조금 더 자랐어요", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            FocusCard {
                SectionTitle("지난 7일 변화", "리포트 보기", onReport)
                Text("지난주보다 총 사용 시간이 12% 감소", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AnalysisScreen(onApp: (AppUsage) -> Unit, onReport: () -> Unit) {
    var period by rememberSaveable { mutableStateOf("오늘") }
    var category by rememberSaveable { mutableStateOf("전체") }
    var sort by rememberSaveable { mutableStateOf("사용 시간") }
    var filterMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val periodApps = DemoData.usageByPeriod(period)
    val periodTotal = periodApps.sumOf { it.minutes }
    val filtered = periodApps.filter { category == "전체" || it.category == category }
        .let { if (sort == "실행 횟수") it.sortedByDescending(AppUsage::launches) else it.sortedByDescending(AppUsage::minutes) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle("사용 분석")
            DemoNotice()
            PeriodSelector(listOf("오늘", "7일", "30일"), period) { period = it }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { AnalysisSummary(period); AnalysisChart(period) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnalysisCategories(periodApps, periodTotal)
                        AnalysisAppList(filtered, periodTotal, category, sort, {filterMenu=true}, {sortMenu=true}, onApp, onReport, filterMenu, sortMenu, {filterMenu=false}, {sortMenu=false}, {category=it}, {sort=it})
                    }
                }
            } else {
                AnalysisSummary(period)
                AnalysisChart(period)
                AnalysisCategories(periodApps, periodTotal)
                AnalysisAppList(filtered, periodTotal, category, sort, {filterMenu=true}, {sortMenu=true}, onApp, onReport, filterMenu, sortMenu, {filterMenu=false}, {sortMenu=false}, {category=it}, {sort=it})
            }
        }
    }
}

@Composable
private fun AnalysisSummary(period: String) {
    val total = when(period) { "오늘" -> DemoData.todayTotalMinutes; "7일" -> DemoData.weeklyTotalMinutes; else -> DemoData.weeklyTotalMinutes * 4 }
    FocusCard {
        Metric(DemoData.minutesLabel(total), "총 사용 시간", if (period == "오늘") "어제보다 18분 감소" else "지난 기간보다 12% 감소")
    }
}

@Composable
private fun AnalysisChart(period: String) {
    val values = if (period == "오늘") DemoData.hourlyUsageMinutes else if (period == "7일") DemoData.dailyUsageMinutes else DemoData.monthlyUsageMinutes
    val labels = if (period == "오늘") List(24) { if (it % 3 == 0) "${it}시" else "" } else if (period == "7일") listOf("월", "화", "수", "목", "금", "토", "일") else List(30) { if (it % 5 == 0) "${it + 1}일" else "" }
    FocusCard {
        SectionTitle(if (period == "오늘") "시간대별 사용량" else "일별 사용 시간")
        BarChart(values, labels, if (period == "오늘") 30 else 180, highlight = if(period == "오늘") 18 else -1)
        Text(if (period == "오늘") "사용 시간이 가장 많은 시각 · 오후 6시" else "요일별 사용 시간 비교", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnalysisCategories(apps: List<AppUsage>, totalMinutes: Int) {
    FocusCard {
        SectionTitle("카테고리별 사용 비중")
        DemoData.categories.forEach { cat ->
            CategoryRow(cat, apps.filter { it.category == cat }.sumOf { it.minutes }, totalMinutes)
        }
    }
}

@Composable
private fun AnalysisAppList(
    apps: List<AppUsage>, totalMinutes: Int, category: String, sort: String,
    openFilter: () -> Unit, openSort: () -> Unit,
    onApp: (AppUsage) -> Unit, onReport: () -> Unit,
    filterExpanded: Boolean, sortExpanded: Boolean, closeFilter: () -> Unit, closeSort: () -> Unit,
    setCategory: (String) -> Unit, setSort: (String) -> Unit
) {
    FocusCard {
        SectionTitle("앱별 사용 시간", "리포트 보기", onReport)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = openFilter) { Text(category) }
                DropdownMenu(filterExpanded, onDismissRequest = closeFilter) {
                    (listOf("전체") + DemoData.categories).forEach { value ->
                        DropdownMenuItem(text = {Text(value)}, onClick = {setCategory(value);closeFilter()})
                    }
                }
            }
            Box {
                OutlinedButton(onClick = openSort) { Text(sort) }
                DropdownMenu(sortExpanded, onDismissRequest = closeSort) {
                    listOf("사용 시간", "실행 횟수").forEach { value ->
                        DropdownMenuItem(text = {Text(value)}, onClick = {setSort(value);closeSort()})
                    }
                }
            }
        }
        if (apps.isEmpty()) {
            Text("선택한 조건의 사용 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            apps.forEachIndexed { index, app ->
                AppUsageRow(app, totalMinutes = totalMinutes, onClick = {onApp(app)})
                if (index < apps.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
