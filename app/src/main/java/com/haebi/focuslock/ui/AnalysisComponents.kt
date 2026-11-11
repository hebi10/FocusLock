package com.haebi.focuslock.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.UsageWindow

@Composable
fun AnalysisChart(snapshot: UsageSnapshot, period: String) {
    val bars = AnalysisModels.bars(snapshot, period)
    var selectedIndex by rememberSaveable(period) { mutableIntStateOf(-1) }
    FocusCard {
        SectionTitle(if (period == "오늘") "시간대별 사용량" else "일별 사용 시간")
        when {
            bars == null -> Text(
                "시간대별 사용 이벤트를 확인할 수 없습니다. 전체 사용 시간은 위 집계를 참고해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            bars.isEmpty() || bars.all { it.minutes == 0 } -> Text(
                "이 기간에 그래프로 표시할 사용 기록이 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                val selected = bars.getOrNull(selectedIndex)
                Text(
                    if (selected == null) "막대를 선택하면 해당 시간의 사용량을 확인할 수 있습니다."
                    else "${selected.detail} · ${UsageFormat.minutesLabel(selected.minutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("analysis-chart-selected")
                )
                val maxValue = bars.maxOf { it.minutes }.coerceAtLeast(1)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val barWidth = if (bars.size <= 7) maxWidth / bars.size else FocusSpacing.minTouchTarget
                    val touchWidth = if (barWidth < FocusSpacing.minTouchTarget) FocusSpacing.minTouchTarget else barWidth
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        bars.forEachIndexed { index, bar ->
                            Column(
                                Modifier.width(touchWidth).heightIn(min = 142.dp)
                                    .testTag("analysis-chart-bar-$index")
                                    .clickable(role = Role.Button) { selectedIndex = index }
                                    .semantics {
                                        contentDescription = "${bar.detail}, ${UsageFormat.minutesLabel(bar.minutes)}"
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().height(110.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val height = (98f * bar.minutes / maxValue).coerceAtLeast(if (bar.minutes > 0) 3f else 0f)
                                    Box(
                                        Modifier.width(20.dp).height(height.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = if (selectedIndex == index) 1f else 0.52f
                                                ),
                                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                            )
                                    )
                                }
                                Text(
                                    bar.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (period == "오늘") Text(
                    "시간대별 정보는 Android에 보관된 앱 전환 이벤트 기반이며 일부 누락될 수 있습니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnalysisCategorySummary(window: UsageWindow) {
    FocusCard {
        SectionTitle("카테고리별 사용 비중")
        val categories = window.apps.groupBy { it.category }
            .map { (category, apps) -> category to apps.sumOf { it.minutes } }
            .sortedByDescending { it.second }
        categories.forEach { (label, minutes) ->
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${UsageFormat.minutesLabel(minutes)} · ${UsageFormat.sharePercent(minutes, window.totalMinutes)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProgressBar(if (window.totalMinutes <= 0) 0f else minutes.toFloat() / window.totalMinutes, neutral = true)
            }
        }
        Text(
            "비율은 선택 기간의 전체 앱 사용 시간을 기준으로 계산합니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnalysisAppList(
    window: UsageWindow,
    period: String,
    excluded: Map<String, Boolean>,
    category: String,
    sort: String,
    query: String,
    onCategory: (String) -> Unit,
    onSort: (String) -> Unit,
    onQuery: (String) -> Unit,
    onApp: (AppUsage, String) -> Unit,
    onReport: () -> Unit
) {
    var categoryMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val available = window.apps.filter { excluded[it.id] != true }
    val categories = AnalysisModels.categories(available)
    val list = AnalysisModels.visibleApps(window.apps, excluded, category, query, sort)
    FocusCard {
        SectionTitle("앱별 사용 시간", "리포트 보기", onReport)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("앱 검색") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "검색어 지우기")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("analysis-search")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { categoryMenu = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget)
                ) {
                    Text("분류: $category", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = { onCategory(item); categoryMenu = false }
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { sortMenu = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget)
                ) {
                    Text("정렬: $sort", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    AnalysisModels.sorts.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = { onSort(item); sortMenu = false }
                        )
                    }
                }
            }
        }
        Text(
            "표시 ${list.size}개 · 검색 대상 ${available.size}개",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("analysis-result-count")
        )
        if (list.isEmpty()) {
            Text(
                if (available.isEmpty()) "표시할 앱 사용 기록이 없습니다."
                else "검색어나 카테고리에 맞는 앱이 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        list.forEachIndexed { index, app ->
            AnalysisAppRow(app, window.totalMinutes) { onApp(app, period) }
            if (index < list.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text(
            "분석에서 제외한 앱도 총 사용 시간에는 포함됩니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalysisAppRow(app: AppUsage, totalMinutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget)
            .testTag("analysis-app-${app.id}")
            .clickable(onClick = onClick)
            .padding(vertical = FocusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
    ) {
        AnalysisAppIcon(app)
        Column(Modifier.weight(1f)) {
            Text(
                app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (app.launches < 0) "실행 횟수 확인 불가" else "실행 ${app.launches}회",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(UsageFormat.minutesLabel(app.minutes), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${UsageFormat.sharePercent(app.minutes, totalMinutes)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalysisAppIcon(app: AppUsage) {
    val context = LocalContext.current
    val icon = remember(context, app.packageName) {
        app.packageName?.let { packageName ->
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName).mutate()
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, 96, 96)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }
    if (icon == null) AppSymbol(app, 40.dp)
    else Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
}
