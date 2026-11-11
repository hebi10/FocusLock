package com.haebi.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.AppPermissionRepository
import com.haebi.focuslock.data.PermissionLookup
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.GoalDayPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppDetailScreen(
    app: AppUsage,
    snapshot: UsageSnapshot,
    period: String,
    onPeriod: (String) -> Unit,
    goalMinutes: Int?,
    isDistraction: Boolean,
    isExcluded: Boolean,
    onBack: () -> Unit,
    onGoal: () -> Unit,
    onSystemSettings: () -> Unit,
    onDistraction: (Boolean) -> Unit,
    onExcluded: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var permissions by remember(app.id) { mutableStateOf<PermissionLookup?>(null) }
    var expanded by remember(app.id) { mutableStateOf(false) }
    LaunchedEffect(app.id) {
        permissions = withContext(Dispatchers.IO) { AppPermissionRepository(context.applicationContext).inspect(app.packageName) }
    }
    val selectedApp = snapshot.window(period).apps.firstOrNull { it.id == app.id }
        ?: app.copy(minutes = 0, launches = -1, lastUsed = "확인 불가")
    val todayMinutes = snapshot.today.apps.firstOrNull { it.id == app.id }?.minutes ?: 0
    val values = when (period) {
        "오늘" -> listOf(todayMinutes)
        "7일" -> snapshot.appDaily7[app.id]
        else -> snapshot.appDaily30[app.id]
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            AppSymbol(app, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    app.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PeriodSelector(listOf("오늘", "7일", "30일"), period, onPeriod)
        FocusCard {
            Metric(UsageFormat.minutesLabel(selectedApp.minutes), "${period} 사용")
            if (goalMinutes != null && goalMinutes > 0) {
                Text(
                    "일일 사용 목표 " + UsageFormat.minutesLabel(goalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (period == "오늘") {
                    ProgressBar(
                        todayMinutes.toFloat() / goalMinutes,
                        isWarning = todayMinutes > goalMinutes
                    )
                    Text(
                        if (todayMinutes > goalMinutes)
                            "오늘 목표보다 " + UsageFormat.minutesLabel(todayMinutes - goalMinutes) + " 초과"
                        else "오늘 목표까지 " + UsageFormat.minutesLabel(goalMinutes - todayMinutes) + " 남음",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (todayMinutes > goalMinutes) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "일일 목표 진행률은 '오늘' 기간에서 확인할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "설정된 일일 사용 목표가 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingRow(
                "사용 목표",
                "시간 초과 시 알림만 제공하며 앱을 강제로 종료하지 않습니다.",
                if (goalMinutes == null) "설정" else "수정",
                onGoal
            )
        }
        FocusCard {
            SectionTitle(if (period == "오늘") "오늘 사용 기록" else "사용 패턴 · 최근 $period")
            if (values == null || values.none { it > 0 }) {
                Text(
                    "선택한 기간에 확인된 앱 사용 기록이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AppDetailHistoryChart(values, snapshot.window(period).startDate)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoPair(
                "실행·전환 이벤트",
                if (selectedApp.launches < 0) "확인 불가" else "${selectedApp.launches}회"
            )
            InfoPair("마지막 사용", selectedApp.lastUsed)
            Text(
                "실행 횟수는 Android에서 조회 가능한 최근 사용 이벤트 기준이며 실제 실행과 차이가 있을 수 있습니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FocusCard {
            SectionTitle("요청 권한")
            Text("표시된 허용 상태는 Android에서 확인 가능한 권한에 한합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (val result = permissions) {
                null -> Text("권한 정보 확인 중", fontSize = 13.sp)
                PermissionLookup.NotVisible -> Text("Android 패키지 가시성 제한으로 이 앱의 권한 정보를 확인할 수 없습니다. 시스템 앱 정보에서 직접 확인해 주세요.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                is PermissionLookup.Found -> {
                    if (result.permissions.isEmpty()) Text("확인 가능한 요청 권한이 없습니다.", fontSize = 13.sp)
                    val shown = if (expanded) result.permissions else result.permissions.take(10)
                    shown.forEachIndexed { index, permission ->
                        InfoPair(permission.name, permission.status)
                        if (index < shown.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (result.permissions.size > 10) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "접기" else "권한 ${result.permissions.size - 10}개 더 보기") }
                }
            }
            SettingRow("Android 앱 권한 확인", "앱 정보 화면에서 자세한 권한을 확인합니다.", onClick = onSystemSettings)
        }
        FocusCard {
            SectionTitle("기기 영향")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("배터리 소비량 측정이 아닌 사용 패턴 정보입니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val referenceTotal = snapshot.window(period).totalMinutes
            InfoPair("해당 기간 앱 사용 비중", "${UsageFormat.sharePercent(selectedApp.minutes, referenceTotal)}%")
            InfoPair("사용 이벤트", if (selectedApp.launches < 0) "확인 불가" else "${selectedApp.launches}회")
            SettingRow("배터리 정보 확인", "시스템 앱 정보 화면에서 배터리 사용량을 확인합니다.", onClick = onSystemSettings)
        }
        FocusCard {
            SectionTitle("앱 관리")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("집중 방해 앱", fontWeight = FontWeight.Medium)
                    Text("집중 중 사용 기록 대상으로 지정 · 자동 차단 없음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isDistraction, onCheckedChange = onDistraction)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("분석에서 제외", fontWeight = FontWeight.Medium)
                    Text("앱 목록에서 숨기며 총 사용 시간에는 포함합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isExcluded, onCheckedChange = onExcluded)
            }
        }
    }
}

@Composable
private fun AppDetailHistoryChart(values: List<Int>, startDate: LocalDate) {
    var selectedIndex by remember(values, startDate) { mutableIntStateOf(values.lastIndex) }
    val maximum = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val selectedDate = startDate.plusDays(selectedIndex.toLong())
    Text(
        "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} · ${UsageFormat.minutesLabel(values[selectedIndex])}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("app-history-selected")
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)
    ) {
        values.forEachIndexed { index, minutes ->
            val date = startDate.plusDays(index.toLong())
            val label = when (values.size) {
                1 -> "오늘"
                7 -> date.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN))
                else -> date.format(DateTimeFormatter.ofPattern("M/d", Locale.KOREAN))
            }
            Column(
                modifier = Modifier.width(FocusSpacing.minTouchTarget).heightIn(min = 146.dp)
                    .testTag("app-history-bar-$index")
                    .clickable(role = Role.Button) { selectedIndex = index }
                    .semantics {
                        contentDescription =
                            "${date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))}, ${UsageFormat.minutesLabel(minutes)}"
                    },
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.fillMaxWidth().height(106.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val height = (94f * minutes / maximum).coerceAtLeast(if (minutes > 0) 3f else 0f)
                    Box(
                        Modifier.width(20.dp).height(height.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (index == selectedIndex) 1f else 0.5f),
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
                Text(
                    label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSheet(
    app: AppUsage,
    current: Int,
    initialWarn80: Boolean,
    initialWarn100: Boolean,
    initialDays: Int,
    onSave: (Int, Boolean, Boolean, Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember(app.id) { mutableIntStateOf(current.coerceIn(15, 240)) }
    var warn80 by remember(app.id) { mutableStateOf(initialWarn80) }
    var warn100 by remember(app.id) { mutableStateOf(initialWarn100) }
    var selectedDays by remember(app.id) { mutableIntStateOf(GoalDayPolicy.normalize(initialDays)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).navigationBarsPadding()
                .padding(horizontal = FocusSpacing.md).padding(bottom = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
            ) {
                Text("사용 목표 설정", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
                    AppSymbol(app)
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                }
                FocusCard {
                    Text("하루 사용 목표", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { minutes = (minutes - 15).coerceAtLeast(15) },
                            modifier = Modifier.weight(1f).heightIn(min = FocusSpacing.minTouchTarget)
                        ) { Text("− 15분") }
                        Text(
                            UsageFormat.minutesLabel(minutes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1.3f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { minutes = (minutes + 15).coerceAtMost(240) },
                            modifier = Modifier.weight(1f).heightIn(min = FocusSpacing.minTouchTarget)
                        ) { Text("+ 15분") }
                    }
                    Text("15분부터 4시간까지 15분 단위로 설정할 수 있습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FocusCard {
                    Text("요일별 알림 적용", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "사용 목표 자체는 저장되며, 선택한 요일에만 자동 목표 경고를 확인합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)
                    ) {
                        (1..4).forEach { value ->
                            val day = DayOfWeek.of(value)
                            GoalWeekdayChip(day, GoalDayPolicy.selected(selectedDays, day),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedDays = GoalDayPolicy.toggle(selectedDays, day) })
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)
                    ) {
                        (5..7).forEach { value ->
                            val day = DayOfWeek.of(value)
                            GoalWeekdayChip(day, GoalDayPolicy.selected(selectedDays, day),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedDays = GoalDayPolicy.toggle(selectedDays, day) })
                        }
                    }
                    TextButton(onClick = {
                        selectedDays = if (selectedDays == GoalDayPolicy.EVERY_DAY)
                            GoalDayPolicy.NONE else GoalDayPolicy.EVERY_DAY
                    }) {
                        Text(if (selectedDays == GoalDayPolicy.EVERY_DAY) "모든 요일 해제" else "모든 요일 선택")
                    }
                    if (selectedDays == GoalDayPolicy.NONE) {
                        Text(
                            "요일을 선택하지 않으면 자동 목표 경고가 발송되지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FocusCard {
                    Text("경고 알림", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("목표 80% 도달", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = warn80, onCheckedChange = { warn80 = it })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("목표 100% 도달", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = warn100, onCheckedChange = { warn100 = it })
                    }
                    Text(
                        "Android 사용 정보와 알림 권한이 허용된 경우에만 주기적으로 확인합니다. 절전 상태에서는 알림이 지연될 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)
            ) {
                ActionButton(
                    "저장",
                    { onSave(minutes, warn80, warn100, selectedDays) },
                    modifier = Modifier.testTag("goal-save")
                )
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.heightIn(min = FocusSpacing.minTouchTarget)
                ) {
                    Text("목표 해제", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GoalWeekdayChip(
    day: DayOfWeek,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val label = when (day) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.heightIn(min = FocusSpacing.minTouchTarget)
            .testTag("goal-day-${day.value}")
    )
}
