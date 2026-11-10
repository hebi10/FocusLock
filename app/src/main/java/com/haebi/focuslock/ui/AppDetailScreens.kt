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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.DemoData

@Composable
fun AppDetailScreen(
    app: AppUsage,
    goalMinutes: Int?,
    isDistraction: Boolean,
    isExcluded: Boolean,
    onBack: () -> Unit,
    onGoal: () -> Unit,
    onSystemSettings: () -> Unit,
    onDistraction: (Boolean) -> Unit,
    onExcluded: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            AppSymbol(app, 48.dp)
            Column {
                Text(app.name, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(app.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        DemoNotice()
        FocusCard {
            Metric(DemoData.minutesLabel(app.minutes), "오늘 사용", "어제보다 12분 감소")
            Text(if(goalMinutes != null) "목표 ${DemoData.minutesLabel(goalMinutes)}" else "설정된 목표 없음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(goalMinutes != null) ProgressBar(app.minutes.toFloat() / goalMinutes.coerceAtLeast(1), isWarning = app.minutes > goalMinutes)
            SettingRow("사용 목표", "목표를 초과해도 앱을 강제 종료하지 않습니다.", "수정", onGoal)
        }
        FocusCard {
            SectionTitle("사용 패턴")
            BarChart(listOf(48, 73, 52, 47, 53, 92, app.minutes), listOf("월", "화", "수", "목", "금", "토", "일"), 120, 6)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoPair("실행·전환 횟수", "${app.launches}회")
            InfoPair("마지막 사용", app.lastUsed)
            InfoPair("자주 사용하는 시간대", "오후 9시")
            Text("실행 횟수는 사용 이벤트를 기준으로 집계할 예정입니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FocusCard {
            SectionTitle("권한")
            Text("요청 권한과 허용 여부는 추후 PackageManager 연동 후 표시됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("카메라", "마이크", "위치", "알림").forEachIndexed { index, label ->
                InfoPair(label, "Android에서 확인")
                if(index < 3) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            SettingRow("Android 앱 권한 확인", "앱 정보 화면에서 권한 항목 확인", onClick = onSystemSettings)
        }
        FocusCard {
            SectionTitle("기기 영향")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("직접 측정한 배터리 소비량이 아닌 사용 패턴 정보입니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InfoPair("전체 사용 시간 비중", "${DemoData.sharePercent(app.minutes, DemoData.todayTotalMinutes)}%")
            InfoPair("장시간 연속 사용", "예시 데이터")
            InfoPair("반복 실행", "${app.launches}회")
            SettingRow("배터리 정보 확인", "앱 정보 화면에서 배터리 항목 확인(기기별 다름)", onClick = onSystemSettings)
        }
        FocusCard {
            SectionTitle("앱 관리")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("집중 방해 앱", fontWeight = FontWeight.Medium)
                    Text("집중 중 사용 기록 대상으로 지정", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isDistraction, onCheckedChange = onDistraction)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("분석에서 제외", fontWeight = FontWeight.Medium)
                    Text("화면 미리보기의 앱 목록에서 숨깁니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isExcluded, onCheckedChange = onExcluded)
            }
        }
        Spacer(Modifier.height(12.dp))
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
                    Text(DemoData.minutesLabel(minutes), fontWeight = FontWeight.Bold)
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
                Text("알림 설정은 UI 시안이며 실제 예약 전송은 추후 연결합니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
