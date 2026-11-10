package com.haebi.focuslock.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.DemoData
import kotlinx.coroutines.delay

private enum class MainTab(val label: String) { HOME("홈"), ANALYSIS("사용 분석"), FOCUS("집중"), GARDEN("정원"), SETTINGS("설정") }

@Composable
fun FocusLockApp() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("focuslock_ui_prototype", 0) }
    var onboarding by rememberSaveable { mutableIntStateOf(if (prefs.getBoolean("onboarding_completed", false)) -1 else 0) }
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme", "시스템") ?: "시스템") }
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    var detail by rememberSaveable { mutableStateOf("") }
    var selectedAppId by rememberSaveable { mutableStateOf("youtube") }
    var goalSheetId by rememberSaveable { mutableStateOf("") }
    var selectedGardenItemId by rememberSaveable { mutableStateOf("") }
    var placedGardenItem by rememberSaveable { mutableStateOf("작은 새싹") }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val goals = remember { mutableStateMapOf("youtube" to 60, "instagram" to 30, "games" to 45) }
    val distraction = remember { mutableStateMapOf("youtube" to true, "instagram" to true, "games" to true) }
    val excluded = remember { mutableStateMapOf<String, Boolean>() }
    val warningAt80 = remember { mutableStateMapOf<String, Boolean>() }
    val warningAt100 = remember { mutableStateMapOf<String, Boolean>() }
    val everydayGoal = remember { mutableStateMapOf<String, Boolean>() }

    var focusPhase by rememberSaveable { mutableStateOf("setup") }
    var focusDuration by rememberSaveable { mutableIntStateOf(25) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }
    var startElapsed by rememberSaveable { mutableLongStateOf(0L) }
    var endElapsed by rememberSaveable { mutableLongStateOf(0L) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var experience by rememberSaveable { mutableIntStateOf(0) }
    var stopConfirm by rememberSaveable { mutableStateOf(false) }

    fun goMain(destination: MainTab) {
        detail = ""
        tab = destination.name
    }
    fun endOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        onboarding = -1
    }
    fun startFocus(minutes: Int) {
        focusDuration = minutes
        remainingSeconds = minutes * 60
        elapsedSeconds = 0
        startElapsed = SystemClock.elapsedRealtime()
        endElapsed = startElapsed + minutes * 60_000L
        paused = false
        completed = false
        detail = ""
        tab = MainTab.FOCUS.name
        focusPhase = "running"
    }
    fun finishFocus() {
        completed = focusDuration == 0 && elapsedSeconds >= 600
        if (completed) experience += 30
        focusPhase = "result"
        stopConfirm = false
    }

    LaunchedEffect(focusPhase, paused) {
        while (focusPhase == "running" && !paused) {
            delay(250)
            val now = SystemClock.elapsedRealtime()
            if (focusDuration == 0) {
                elapsedSeconds = ((now - startElapsed) / 1000).toInt().coerceAtLeast(0)
            } else {
                remainingSeconds = ((endElapsed - now + 999) / 1000).toInt().coerceAtLeast(0)
                elapsedSeconds = focusDuration * 60 - remainingSeconds
                if (remainingSeconds == 0) {
                    completed = true
                    experience += 30
                    focusPhase = "result"
                }
            }
        }
    }

    BackHandler(enabled = onboarding >= 0 || detail.isNotEmpty() || focusPhase != "setup") {
        when {
            onboarding > 0 -> onboarding--
            onboarding == 0 -> endOnboarding()
            focusPhase == "running" -> stopConfirm = true
            focusPhase == "result" -> { focusPhase = "setup"; goMain(MainTab.HOME) }
            detail.isNotEmpty() -> detail = ""
        }
    }

    FocusLockTheme(theme) {
        val immersive = onboarding >= 0 || focusPhase == "running" || focusPhase == "result"
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!immersive) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        MainTab.entries.forEach { item ->
                            val icon = when(item) {
                                MainTab.HOME -> Icons.Outlined.Home
                                MainTab.ANALYSIS -> Icons.Outlined.Assessment
                                MainTab.FOCUS -> Icons.Outlined.TrackChanges
                                MainTab.GARDEN -> Icons.Outlined.Yard
                                MainTab.SETTINGS -> Icons.Outlined.Settings
                            }
                            NavigationBarItem(
                                selected = tab == item.name,
                                onClick = { goMain(item) },
                                icon = { Icon(icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { inset ->
            Box(Modifier.padding(inset).fillMaxSize()) {
                when {
                    onboarding == 0 -> OnboardingIntro(
                        onNext = { onboarding = 1 }, onExplore = ::endOnboarding
                    )
                    onboarding == 1 -> OnboardingPermission(
                        onOpenSettings = { openUsageSettings(context) },
                        onNext = { onboarding = 2 }
                    )
                    onboarding == 2 -> OnboardingGoal(
                        goals = goals,
                        onSetGoal = { goalSheetId = it.id },
                        onFinish = ::endOnboarding
                    )
                    focusPhase == "running" -> FocusRunningScreen(
                        minutes = focusDuration,
                        seconds = if(focusDuration == 0) elapsedSeconds else remainingSeconds,
                        paused = paused,
                        onPauseToggle = {
                            if(paused) {
                                val now = SystemClock.elapsedRealtime()
                                if(focusDuration == 0) startElapsed = now - elapsedSeconds * 1000L
                                else endElapsed = now + remainingSeconds * 1000L
                            }
                            paused = !paused
                        },
                        onFinish = { stopConfirm = true }
                    )
                    focusPhase == "result" -> FocusResultScreen(completed, elapsedSeconds,
                        onHome = {focusPhase="setup";goMain(MainTab.HOME)},
                        onGarden = {focusPhase="setup";goMain(MainTab.GARDEN)})
                    detail == "app" -> AppDetailScreen(
                        app = DemoData.appById(selectedAppId),
                        goalMinutes = goals[selectedAppId],
                        isDistraction = distraction[selectedAppId] ?: false,
                        isExcluded = excluded[selectedAppId] ?: false,
                        onBack = { detail = "" },
                        onGoal = { goalSheetId = selectedAppId },
                        onSystemSettings = {
                            openInstalledAppInfo(context, DemoData.appById(selectedAppId).packageName)
                        },
                        onDistraction = { distraction[selectedAppId] = it },
                        onExcluded = { excluded[selectedAppId] = it }
                    )
                    detail == "report" -> ReportScreen(
                        onBack = {detail = ""}, onApp = { selectedAppId=it.id;detail="app" }
                    )
                    detail == "permissions" -> PermissionManagementScreen(
                        onBack = {detail = ""},
                        onUsageSettings = {openUsageSettings(context)},
                        onNotificationSettings = {openNotificationSettings(context)}
                    )
                    else -> when(MainTab.valueOf(tab)) {
                        MainTab.HOME -> HomeScreen(
                            goals = goals,
                            onFocus = {goMain(MainTab.FOCUS)},
                            onAnalysis = {goMain(MainTab.ANALYSIS)},
                            onApp = { selectedAppId=it.id;detail="app" },
                            onGarden = {goMain(MainTab.GARDEN)},
                            onReport = {detail="report"},
                            onSettings = {goMain(MainTab.SETTINGS)}
                        )
                        MainTab.ANALYSIS -> AnalysisScreen(
                            onApp = { selectedAppId=it.id;detail="app" }, onReport = {detail="report"}
                        )
                        MainTab.FOCUS -> FocusSetupScreen(
                            onStart = ::startFocus,
                            selectedDistractions = distraction.values.count { it }
                        )
                        MainTab.GARDEN -> GardenScreen(
                            experience = experience,
                            placedItem = placedGardenItem,
                            onItem = {selectedGardenItemId=it.id},
                            onFocus = {goMain(MainTab.FOCUS)}
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            theme = theme,
                            onTheme = { theme=it;prefs.edit().putString("theme",it).apply() },
                            onPermissions = {detail="permissions"},
                            onInfo = {showInfo=true}
                        )
                    }
                }
            }
        }
        if(goalSheetId.isNotEmpty()) {
            val app = DemoData.appById(goalSheetId)
            GoalSheet(
                app=app,
                current=goals[app.id] ?: 60,
                initialWarn80=warningAt80[app.id] ?: true,
                initialWarn100=warningAt100[app.id] ?: true,
                initialEveryday=everydayGoal[app.id] ?: true,
                onSave = { minutes, warn80, warn100, daily ->
                    goals[app.id] = minutes
                    warningAt80[app.id] = warn80
                    warningAt100[app.id] = warn100
                    everydayGoal[app.id] = daily
                    goalSheetId = ""
                },
                onClear = {goals.remove(app.id);goalSheetId=""},
                onDismiss = {goalSheetId=""}
            )
        }
        if(selectedGardenItemId.isNotEmpty()) {
            val item = listOf(
                GardenItem("sprout", "작은 새싹", "🌱", 0),
                GardenItem("flowers", "꽃밭", "🌼", 60),
                GardenItem("tree", "작은 나무", "🌳", 150),
                GardenItem("bench", "정원 벤치", "🪑", 300)
            ).firstOrNull { it.id == selectedGardenItemId }
            if (item != null) GardenItemSheet(item, experience,
                onDismiss={selectedGardenItemId=""}, onSelect={placedGardenItem=item.name;selectedGardenItemId=""})
        }
        if(stopConfirm) AlertDialog(
            onDismissRequest={stopConfirm=false},
            title={Text("집중을 종료할까요?")},
            text={Text(if(focusDuration==0) "10분 이상 진행했다면 완료 보상을 받습니다." else "설정한 시간을 채우기 전 종료하면 완료 보상은 지급되지 않습니다.")},
            confirmButton={TextButton(onClick=::finishFocus){Text("종료")}},
            dismissButton={TextButton(onClick={stopConfirm=false}){Text("계속 집중")}}
        )
        if(showInfo) AlertDialog(
            onDismissRequest={showInfo=false},
            title={Text("FocusLock UI 프로토타입")},
            text={Text("001_전체기능_및_화면_설계 단계입니다. 앱 사용 통계 수집, 알림 예약, 보상 영구 저장, 앱 제한은 구현되지 않았습니다.")},
            confirmButton={TextButton(onClick={showInfo=false}){Text("확인")}}
        )
    }
}
