package com.haebi.focuslock.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat
import com.haebi.focuslock.data.UsageLoadState

@Composable
private fun OnboardingLayout(
    step: Int,
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize()
                .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm)
                .testTag("onboarding-step-$step")
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
            ) {
                Spacer(Modifier.height(FocusSpacing.xs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FocusLock", style = MaterialTheme.typography.titleMedium)
                    Text("$step / 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().height(FocusSpacing.xxs),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(FocusSpacing.xs))
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                body()
                Spacer(Modifier.height(FocusSpacing.sm))
            }
            Column(
                Modifier.fillMaxWidth().padding(top = FocusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)
            ) {
                ActionButton(primaryLabel, onPrimary)
                TextButton(
                    onClick = onSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = FocusSpacing.minTouchTarget)
                ) {
                    Text(secondaryLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeature(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(FocusSpacing.icon))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FocusSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OnboardingIntro(onNext: () -> Unit, onExplore: () -> Unit) {
    OnboardingLayout(
        step = 1,
        title = OnboardingCopyPolicy.introTitle,
        description = OnboardingCopyPolicy.introDescription,
        primaryLabel = "시작하기",
        onPrimary = onNext,
        secondaryLabel = "둘러보기",
        onSecondary = onExplore
    ) {
        FocusCard {
            OnboardingFeature("앱별 사용 패턴", "오늘·최근 7일·30일 사용 시간과 실제 기록을 확인합니다.", Icons.Outlined.Assessment)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OnboardingFeature("선택형 사용 목표", "필요할 때 앱별 목표와 요일별 알림을 설정합니다.", Icons.Outlined.TrackChanges)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OnboardingFeature("집중 기록과 정원", "집중 타이머는 선택해서 사용하며 완료 기록은 기기에 저장합니다.", Icons.Outlined.Yard)
        }
    }
}

@Composable
fun OnboardingPermission(
    permissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingLayout(
        step = 2,
        title = "사용 정보 접근",
        description = OnboardingCopyPolicy.permissionDescription,
        primaryLabel = if (permissionGranted) "목표 설정으로" else "권한 설정 열기",
        onPrimary = if (permissionGranted) onNext else onOpenSettings,
        secondaryLabel = "나중에",
        onSecondary = onNext
    ) {
        FocusCard {
            OnboardingFeature(
                if (permissionGranted) "사용 정보 접근 허용됨" else "사용 정보 접근 미허용",
                if (permissionGranted) "앱으로 돌아오면 기록을 다시 확인합니다." else "권한은 Android 설정에서 직접 선택할 수 있습니다.",
                if (permissionGranted) Icons.Outlined.CheckCircleOutline else Icons.Outlined.LockOpen
            )
        }
        FocusCard {
            Text("권한을 사용하는 기능", style = MaterialTheme.typography.titleMedium)
            OnboardingFeature("앱 사용 시간 분석", "앱별 기록과 사용 패턴을 확인합니다.", Icons.Outlined.Assessment)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OnboardingFeature("사용 목표 경고", "목표 시간 확인에는 사용 정보 접근이, 알림 발송에는 별도 알림 권한이 필요합니다.", Icons.Outlined.NotificationsNone)
        }
        Text(
            OnboardingCopyPolicy.permissionNotice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OnboardingGoal(
    apps: List<AppUsage>,
    goals: Map<String, Int>,
    usageState: UsageLoadState,
    sourceLabel: String,
    onSetGoal: (AppUsage) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onFinish: () -> Unit
) {
    OnboardingLayout(
        step = 3,
        title = OnboardingCopyPolicy.goalTitle,
        description = OnboardingCopyPolicy.goalDescription,
        primaryLabel = "FocusLock 시작",
        onPrimary = onFinish,
        secondaryLabel = "나중에 설정",
        onSecondary = onFinish
    ) {
        FocusCard {
            Text("관리할 앱 선택", style = MaterialTheme.typography.titleMedium)
            when (usageState) {
                UsageLoadState.Loading -> Text(
                    "Android 사용 기록을 확인하는 중입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UsageLoadState.PermissionRequired -> {
                    Text("사용 정보 접근 권한이 없어 앱 목록을 표시할 수 없습니다. 권한 없이 시작할 수도 있습니다.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = FocusSpacing.minTouchTarget)) {
                        Text("권한 설정 열기")
                    }
                }
                is UsageLoadState.Unavailable -> {
                    Text(usageState.reason, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = FocusSpacing.minTouchTarget)) {
                        Text("다시 확인")
                    }
                }
                is UsageLoadState.Ready -> {
                    if (apps.isEmpty()) {
                        Text("아직 확인된 앱 사용 기록이 없습니다. 다른 앱을 사용한 뒤 돌아와 목표를 설정할 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = FocusSpacing.minTouchTarget)) {
                            Text("다시 확인")
                        }
                    } else {
                        Text(sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        apps.take(5).forEach { app ->
                            val savedGoal = goals[app.id]
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                            ) {
                                AppSymbol(app)
                                Text(app.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                TextButton(
                                    onClick = { onSetGoal(app) },
                                    modifier = Modifier.heightIn(min = FocusSpacing.minTouchTarget)
                                ) {
                                    Text(if (savedGoal == null) "목표 설정" else UsageFormat.minutesLabel(savedGoal))
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            OnboardingCopyPolicy.goalNotice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
