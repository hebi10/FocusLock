package com.haebi.focuslock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.FocusSummary

@Composable
fun ReportScreen(state: UsageLoadState, excluded: Map<String, Boolean>, focusSummary: FocusSummary, onBack: (() -> Unit)?, onApp: (AppUsage, String) -> Unit, onUsageSettings: () -> Unit, onRefresh: () -> Unit) {
    var period by rememberSaveable { mutableStateOf("7일") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            }
            Text("사용 리포트", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        PeriodSelector(listOf("7일", "30일"), period) { period = it }
        val snapshot = (state as? UsageLoadState.Ready)?.snapshot
        if (snapshot == null) {
            when (state) {
                UsageLoadState.PermissionRequired -> InfoState("사용 정보 접근 필요", "리포트를 보려면 Android 사용 정보 접근이 필요합니다.", "권한 설정 열기", onUsageSettings)
                UsageLoadState.Loading -> InfoState("기록 확인 중", "사용 정보를 불러오고 있습니다.")
                is UsageLoadState.Unavailable -> InfoState("사용 기록 확인 실패", state.reason, "다시 시도", onRefresh)
                else -> Unit
            }
        } else {
            val selected = snapshot.window(period)
            val prev = snapshot.prior(period)
            val visibleApps = selected.apps.filter { excluded[it.id] != true }
            FocusCard {
                Metric(UsageFormat.minutesLabel(selected.totalMinutes), "최근 ${period} 총 사용 시간")
                Text(UsageFormat.comparison(selected.totalMinutes, prev.totalMinutes) ?: "비교할 이전 기록이 없습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FocusCard {
                SectionTitle("사용 시간 추이")
                val values = snapshot.chart(period) ?: emptyList()
                if (values.any { it > 0 }) {
                    val labels = if (period == "7일") listOf("6일 전", "5일 전", "4일 전", "3일 전", "2일 전", "어제", "오늘") else thirtyDayAxisLabels(values.size)
                    BarChart(values, labels, (values.maxOrNull() ?: 1).coerceAtLeast(1), -1)
                    if (period == "30일") {
                        Text("가로축 숫자는 오늘 기준 며칠 전인지를 표시합니다. (0 = 오늘)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else Text("선택 기간의 일별 사용 기록이 없습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FocusCard {
                SectionTitle("사용량 상위 앱")
                if (visibleApps.isEmpty()) Text(if (selected.apps.isEmpty()) "선택 기간의 앱 사용 기록이 없습니다." else "분석 제외 설정으로 표시할 앱이 없습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                visibleApps.take(5).forEachIndexed { index, app ->
                    AppUsageRow(app, totalMinutes = selected.totalMinutes, onClick = { onApp(app, period) })
                    if(index < minOf(5, visibleApps.size) - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        FocusCard {
            SectionTitle("집중 성과 · ${period}")
            val focus = focusSummary.forPeriod(period)
            if (focus.completedCount == 0) {
                Text("선택 기간에 완료된 집중 세션이 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InfoPairReport("완료 세션", "${focus.completedCount}회")
                InfoPairReport("완료 집중 시간", UsageFormat.minutesLabel(focus.minutes))
                InfoPairReport("획득 경험치", "${focus.experience} XP")
                Text("선택한 기간의 완료 기록만 집계합니다. 중단된 세션은 완료 횟수와 경험치에 포함하지 않습니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoPairReport(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsScreen(theme: String, onTheme: (String) -> Unit, onPermissions: () -> Unit, onInfo: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("설정", "FocusLock의 사용 환경을 관리합니다")
        FocusCard {
            SectionTitle("권한")
            SettingRow("사용 정보·알림 권한 관리", "권한별 허용 상태, 용도 및 시스템 설정 확인", onClick = onPermissions)
        }
        FocusCard {
            SectionTitle("화면")
            Text("테마", fontSize = 13.sp)
            PeriodSelector(listOf("시스템", "라이트", "다크"), theme, onTheme)
        }
        FocusCard {
            SectionTitle("알림")
            Text("앱별 목표 설정에서 80%·100% 경고 및 적용 요일을 선택합니다. WorkManager는 최소 약 15분 간격으로 확인하며 Android 절전 정책에 따라 알림이 지연될 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FocusCard {
            SectionTitle("데이터")
            Text("사용 기록은 Android 시스템에서 읽으며, FocusLock의 집중 세션·보상과 목표 설정은 기기에 저장됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("전체 데이터 삭제 기능은 현재 제공하지 않습니다. Android 앱 정보의 저장공간 메뉴에서 앱 데이터 초기화가 가능하지만 모든 앱 설정과 기록이 삭제됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FocusCard {
            SectionTitle("앱 정보")
            SettingRow("현재 구현 범위", "실제 사용 통계·권한·목표 경고·집중 기록 저장", onClick = onInfo)
            Text("FocusLock 0.3.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PermissionManagementScreen(onBack: () -> Unit, onUsageSettings: () -> Unit, onNotificationSettings: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val usageGranted = remember(refresh) { hasUsageAccess(context) }
    val notificationGranted = remember(refresh) { hasNotificationAccess(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            Text("권한 관리", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { refresh++ }) { Icon(Icons.Outlined.Refresh, "권한 새로고침") }
        }
        InfoState("개인정보 보호", "사용 기록은 Android 시스템에서 읽고 기기 내부에만 표시합니다. 외부 서버에 전송하지 않습니다.")
        FocusCard {
            SectionTitle("사용 정보 접근")
            Text(if(usageGranted) "현재 상태: 허용" else "현재 상태: 미허용", color = if(usageGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("앱별 사용 통계를 조회하려면 Android 특수 앱 접근 권한이 필요합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionButton("Android 사용 정보 접근 설정", onUsageSettings)
        }
        FocusCard {
            SectionTitle("알림")
            Text(if(notificationGranted) "현재 상태: 허용" else "현재 상태: 미허용", color = if(notificationGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("앱별 목표 도달 알림을 받으려면 허용이 필요합니다. Android 백그라운드 제한으로 알림이 지연될 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onNotificationSettings, modifier = Modifier.fillMaxWidth()) { Text("Android 알림 설정") }
        }
        Text("Android 설정에서 돌아오면 권한 상태를 다시 확인합니다. 표시가 갱신되지 않으면 상단 새로고침을 눌러주세요.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun thirtyDayAxisLabels(count: Int): List<String> =
    List(count) { index ->
        if (index % 5 == 0 || index == count - 1) (count - index - 1).toString() else ""
    }
