package com.haebi.focuslock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.R
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.AppUsage

@Composable
fun OnboardingIntro(onNext: () -> Unit, onExplore: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("FocusLock", fontSize = 35.sp, fontWeight = FontWeight.Bold)
            Text("내 스마트폰 습관을 차분하게 관리하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.garden_illustration), contentDescription = "작은 정원과 캐릭터", modifier = Modifier.fillMaxWidth().height(180.dp))
            FocusCard {
                IntroFeature("앱별 사용 시간 확인", "어떤 앱에 시간을 쓰는지 한눈에 파악해요.", Icons.Outlined.Assessment)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IntroFeature("집중 목표 설정", "원하는 만큼 집중 시간을 정하고 실천해요.", Icons.Outlined.TrackChanges)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IntroFeature("집중하면 정원이 성장", "집중한 시간이 쌓일수록 작은 정원이 자라요.", Icons.Outlined.Yard)
            }
            Text("●  ○  ○", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("시작하기", onNext)
            TextButton(onClick = onExplore, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("둘러보기", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun IntroFeature(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OnboardingPermission(onOpenSettings: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("FocusLock", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("2 / 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("사용 기록 접근 권한", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("앱별 사용 시간을 확인하려면 Android 사용 정보 접근 권한이 필요합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.garden_illustration), contentDescription = "정원 이미지", modifier = Modifier.fillMaxWidth().height(150.dp))
            FocusCard {
                IntroFeature("사용 시간 분석", "앱별로 얼마나 사용했는지 확인해요.", Icons.Outlined.Assessment)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IntroFeature("목표 초과 알림", "설정한 목표 시간을 넘으면 안내해요.", Icons.Outlined.NotificationsNone)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IntroFeature("집중 중 방해 앱 기록", "확인 가능한 사용 이벤트를 분석해요.", Icons.Outlined.TrackChanges)
            }
            FocusCard {
                IntroFeature("기록은 기기 안에서 처리", "Android 사용 기록은 기기 내부에서만 읽으며 서버로 전송하지 않습니다.", Icons.Outlined.CheckCircleOutline)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("권한 설정 열기", onOpenSettings)
            TextButton(onClick = onNext, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("나중에", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun OnboardingGoal(apps: List<AppUsage>, goals: Map<String, Int>, onSetGoal: (AppUsage) -> Unit, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("FocusLock", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("3 / 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.garden_illustration), contentDescription = "작은 정원", modifier = Modifier.fillMaxWidth().height(160.dp))
            Text("첫 목표 설정", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("자주 사용하는 앱의 하루 목표를 정해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (apps.isEmpty()) Text("선택 가능한 사용 기록이 없습니다. 사용 정보 접근을 허용한 뒤 설정에서 목표를 추가할 수 있습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            apps.take(5).forEach { app ->
                val id = app.id
                FocusCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AppSymbol(app)
                        Spacer(Modifier.width(12.dp))
                        Text(app.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onSetGoal(app) }) { Text(if (goals[id] == null) "목표 설정" else UsageFormat.minutesLabel(goals[id] ?: 60)) }
                    }
                }
            }
            Text("언제든 설정에서 변경할 수 있어요.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("FocusLock 시작", onFinish)
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("나중에 설정", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
