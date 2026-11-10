package com.haebi.focuslock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.AppPermissionRepository
import com.haebi.focuslock.data.PermissionLookup
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppDetailScreen(
    app: AppUsage,
    snapshot: UsageSnapshot,
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
    val history = snapshot.appDaily7[app.id]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            AppSymbol(app, 48.dp)
            Column {
                Text(app.name, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(app.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        FocusCard {
            Metric(UsageFormat.minutesLabel(app.minutes), "선택한 기간 사용")
            Text(if (goalMinutes != null) "일일 목표 ${UsageFormat.minutesLabel(goalMinutes)}" else "설정된 일일 목표 없음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (goalMinutes != null) Text("목표 달성률은 사용 분석의 ‘오늘’에서 확인합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingRow("사용 목표", "목표를 초과해도 다른 앱을 강제 종료하지 않습니다.", "수정", onGoal)
        }
        FocusCard {
            SectionTitle("사용 패턴 · 최근 7일")
            if (!history.isNullOrEmpty()) BarChart(history, listOf("월", "화", "수", "목", "금", "토", "일"), (history.maxOrNull() ?: 1).coerceAtLeast(1), -1)
            else Text("기간별 앱 사용 기록 없음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoPair("실행·전환 이벤트", if (app.launches < 0) "확인 불가" else "${app.launches}회")
            InfoPair("마지막 사용", app.lastUsed)
            Text("실행 횟수는 Android에 남아 있는 최근 사용 이벤트를 기반으로 집계하며 일부 누락될 수 있습니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            val referenceTotal = listOf(snapshot.today, snapshot.last7, snapshot.last30)
                .firstOrNull { window -> window.apps.any { it.id == app.id && it.minutes == app.minutes } }?.totalMinutes ?: snapshot.today.totalMinutes
            InfoPair("해당 기간 앱 사용 비중", "${UsageFormat.sharePercent(app.minutes, referenceTotal)}%")
            InfoPair("사용 이벤트", if (app.launches < 0) "확인 불가" else "${app.launches}회")
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
                    Text("통계·상위 앱 목록에서 숨깁니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isExcluded, onCheckedChange = onExcluded)
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
    initialEveryday: Boolean,
    onSave: (Int, Boolean, Boolean, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember(app.id) { mutableIntStateOf(current.coerceIn(15, 240)) }
    var warn80 by remember(app.id) { mutableStateOf(initialWarn80) }
    var warn100 by remember(app.id) { mutableStateOf(initialWarn100) }
    var weekdays by remember(app.id) { mutableStateOf(initialEveryday) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("사용 목표 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSymbol(app)
                Text(app.name, fontWeight = FontWeight.SemiBold)
            }
            FocusCard {
                Text("하루 제한 시간", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { minutes = (minutes - 15).coerceAtLeast(15) }) { Text("− 15분") }
                    Text(UsageFormat.minutesLabel(minutes), fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { minutes = (minutes + 15).coerceAtMost(240) }) { Text("+ 15분") }
                }
            }
            FocusCard {
                Text("경고 알림", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("80% 도달", fontSize = 13.sp)
                    Switch(checked = warn80, onCheckedChange = { warn80 = it })
                    Text("100% 도달", fontSize = 13.sp)
                    Switch(checked = warn100, onCheckedChange = { warn100 = it })
                }
                Text("경고 알림은 기능 구현 전 상태입니다. 현재는 설정만 저장됩니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FocusCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("매일 적용", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = weekdays, onCheckedChange = { weekdays = it })
                }
                if(!weekdays) Text("요일별 목표 선택은 후속 기능 단계에서 연동합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ActionButton("저장", { onSave(minutes, warn80, warn100, weekdays) })
            TextButton(onClick = onClear, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("목표 해제", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
