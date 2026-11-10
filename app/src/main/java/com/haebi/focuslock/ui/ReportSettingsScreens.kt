package com.haebi.focuslock.ui

import android.content.Context
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
import com.haebi.focuslock.data.DemoData

@Composable
fun ReportScreen(onBack: () -> Unit, onApp: (AppUsage) -> Unit) {
    var period by rememberSaveable { mutableStateOf("7일") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            Text("사용 리포트", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        DemoNotice()
        PeriodSelector(listOf("7일", "30일"), period) { period = it }
        FocusCard {
            Metric(if(period == "7일") DemoData.minutesLabel(DemoData.weeklyTotalMinutes) else DemoData.minutesLabel(DemoData.monthlyTotalMinutes), "총 사용 시간", "이전 기간보다 12% 감소")
            ProgressBar(0.67f)
        }
        FocusCard {
            SectionTitle("사용 시간 추이")
            if(period == "7일") BarChart(DemoData.dailyUsageMinutes, listOf("월", "화", "수", "목", "금", "토", "일"), 180, 4)
            else BarChart(DemoData.monthlyUsageMinutes, List(30) { if(it%5==0) "${it+1}일" else "" }, 180, -1)
        }
        FocusCard {
            SectionTitle("이번 기간의 집중 성과")
            ReportPair("집중 시간", "2시간 30분")
            ReportPair("목표 달성", "5일")
            ReportPair("집중 성공률", "80%")
            Text("위 집중 성과는 화면 구성 확인용 예시입니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FocusCard {
            SectionTitle("사용량 상위 앱")
            DemoData.usageByPeriod(period).take(3).forEachIndexed { index, app ->
                AppUsageRow(app, totalMinutes = if(period == "7일") DemoData.weeklyTotalMinutes else DemoData.monthlyTotalMinutes, onClick = { onApp(app) })
                if(index<2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ReportPair(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SettingsScreen(theme: String, onTheme: (String) -> Unit, onPermissions: () -> Unit, onInfo: () -> Unit) {
    var notifyEnabled by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("설정", "FocusLock의 사용 환경을 관리합니다")
        FocusCard {
            SectionTitle("권한")
            SettingRow("사용 정보 및 알림 권한", "현재 허용 상태 확인과 Android 설정으로 이동", onClick = onPermissions)
        }
        FocusCard {
            SectionTitle("화면")
            Text("테마", fontSize = 13.sp)
            PeriodSelector(listOf("시스템", "라이트", "다크"), theme, onTheme)
        }
        FocusCard {
            SectionTitle("알림")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("목표 알림", fontWeight = FontWeight.Medium)
                    Text("설정 미리보기 · 실제 알림 전송 미연동", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
            }
        }
        FocusCard {
            SectionTitle("데이터")
            Text("앱 사용 기록은 아직 연결되지 않았습니다. 실제 데이터 저장·초기화 기능은 후속 개발 단계에 포함합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FocusCard {
            SectionTitle("앱 정보")
            SettingRow("프로토타입 정보", "디자인 구현 범위 및 개인정보 원칙", onClick = onInfo)
            Text("FocusLock 0.1.0-ui", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        InfoState("개인정보 보호", "사용 기록은 후속 구현에서 기기 내부에서 처리할 예정입니다. 현재 프로젝트는 UI 데모이며 기록을 수집하지 않습니다.")
        FocusCard {
            SectionTitle("사용 정보 접근")
            Text(if(usageGranted) "현재 상태: 허용" else "현재 상태: 미허용", color = if(usageGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("앱별 사용 통계를 조회하려면 Android의 특수 앱 접근 권한이 필요합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionButton("Android 사용 정보 접근 설정", onUsageSettings)
        }
        FocusCard {
            SectionTitle("알림")
            Text(if(notificationGranted) "현재 상태: 허용" else "현재 상태: 미허용", color = if(notificationGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("실제 목표 경고 알림의 발송은 후속 기능 단계에서 구현합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onNotificationSettings, modifier = Modifier.fillMaxWidth()) { Text("Android 알림 설정") }
        }
        Text("시스템 설정에서 돌아온 후 우측 새로고침 버튼으로 권한 상태를 확인할 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
