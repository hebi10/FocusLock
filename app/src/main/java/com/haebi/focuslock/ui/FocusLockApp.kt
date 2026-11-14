package com.haebi.focuslock.ui

import android.os.SystemClock
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageRepository
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.FocusSessionEntity
import com.haebi.focuslock.data.FocusSessionStatus
import com.haebi.focuslock.data.FocusSessionStore
import com.haebi.focuslock.data.FocusSummary
import com.haebi.focuslock.data.GoalAlerts
import com.haebi.focuslock.data.GoalDayPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class MainTab(val label: String) {
    HOME("홈"), ANALYSIS("사용 분석"), REPORT("리포트"), FOCUS("집중"), SETTINGS("설정")
}

internal fun restoredMainTab(value: String): MainTab = when (value) {
    "GARDEN" -> MainTab.FOCUS
    else -> MainTab.entries.firstOrNull { it.name == value } ?: MainTab.HOME
}

@Composable
fun FocusLockApp() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("focuslock_ui_prototype", 0) }
    val repository = remember(context) { UsageRepository(context.applicationContext) }
    val focusStore = remember(context) { FocusSessionStore.onDevice(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var goalSettingsVersion by remember { mutableIntStateOf(0) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reload ->
        if (reload) goalSettingsVersion++
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadToken by remember { mutableIntStateOf(0) }
    val usageAccessGranted = remember(reloadToken) { hasUsageAccess(context) }
    var usageState by remember { mutableStateOf<UsageLoadState>(UsageLoadState.Loading) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reloadToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(reloadToken) {
        usageState = UsageLoadState.Loading
        usageState = withContext(Dispatchers.IO) { repository.load() }
    }
    val usageSnapshot = (usageState as? UsageLoadState.Ready)?.snapshot
    var onboarding by rememberSaveable { mutableIntStateOf(if (prefs.getBoolean("onboarding_completed", false)) -1 else 0) }
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme", "시스템") ?: "시스템") }
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    var focusGardenOpen by rememberSaveable { mutableStateOf(tab == "GARDEN") }
    LaunchedEffect(tab) {
        if (tab == "GARDEN") {
            focusGardenOpen = true
            tab = MainTab.FOCUS.name
        }
    }
    var detail by rememberSaveable { mutableStateOf("") }
    var selectedAppId by rememberSaveable { mutableStateOf("") }
    var selectedAppPeriod by rememberSaveable { mutableStateOf("오늘") }
    var selectedAppRecord by remember { mutableStateOf<AppUsage?>(null) }
    var goalSheetId by rememberSaveable { mutableStateOf("") }
    var selectedGardenItemId by rememberSaveable { mutableStateOf("") }
    var placedGardenItemId by rememberSaveable {
        mutableStateOf(
            GardenCatalog.fromSaved(
                prefs.getString("placed_garden_item_id", null)
                    ?: prefs.getString("placed_garden_item", null)
            ).id
        )
    }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val goals = remember(prefs) {
        mutableStateMapOf<String, Int>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("goal.") && value is Int) map[key.removePrefix("goal.")] = value }
        }
    }
    val distraction = remember(prefs) {
        mutableStateMapOf<String, Boolean>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("distraction.") && value is Boolean) map[key.removePrefix("distraction.")] = value }
        }
    }
    val excluded = remember(prefs) {
        mutableStateMapOf<String, Boolean>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("excluded.") && value is Boolean) map[key.removePrefix("excluded.")] = value }
        }
    }
    val warningAt80 = remember(prefs) {
        mutableStateMapOf<String, Boolean>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("warn80.") && value is Boolean) map[key.removePrefix("warn80.")] = value }
        }
    }
    val warningAt100 = remember(prefs) {
        mutableStateMapOf<String, Boolean>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("warn100.") && value is Boolean) map[key.removePrefix("warn100.")] = value }
        }
    }
    val everydayGoal = remember(prefs) {
        mutableStateMapOf<String, Boolean>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("everyday.") && value is Boolean) map[key.removePrefix("everyday.")] = value }
        }
    }
    val goalDays = remember(prefs) {
        mutableStateMapOf<String, Int>().also { map ->
            prefs.all.forEach { (key, value) -> if (key.startsWith("goalDays.") && value is Int) map[key.removePrefix("goalDays.")] = GoalDayPolicy.normalize(value) }
        }
    }

    var focusPhase by rememberSaveable { mutableStateOf("setup") }
    var focusDuration by rememberSaveable { mutableIntStateOf(25) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }
    var startElapsed by rememberSaveable { mutableLongStateOf(0L) }
    var endElapsed by rememberSaveable { mutableLongStateOf(0L) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var experience by rememberSaveable { mutableIntStateOf(0) }
    var growthPoints by remember { mutableIntStateOf(0) }
    var resultExperience by rememberSaveable { mutableIntStateOf(0) }
    var resultPoints by rememberSaveable { mutableIntStateOf(0) }
    var startInProgress by remember { mutableStateOf(false) }
    var finishInProgress by remember { mutableStateOf(false) }
    var todayExperience by remember { mutableIntStateOf(0) }
    var stopConfirm by rememberSaveable { mutableStateOf(false) }
    var focusSessionId by remember { mutableStateOf<String?>(null) }
    var focusRestoreMessage by remember { mutableStateOf(false) }
    var focusReport by remember { mutableStateOf(FocusSummary()) }

    fun applySession(session: FocusSessionEntity) {
        focusSessionId = session.id
        focusDuration = session.durationMinutes
        elapsedSeconds = session.elapsedSeconds
        remainingSeconds = if (session.durationMinutes == 0) 0 else
            (session.durationMinutes * 60 - session.elapsedSeconds).coerceAtLeast(0)
        startElapsed = session.checkpointElapsedRealtime - session.elapsedSeconds * 1000L
        endElapsed = session.checkpointElapsedRealtime + remainingSeconds * 1000L
        paused = session.status == FocusSessionStatus.PAUSED
        focusPhase = "running"
    }

    LaunchedEffect(reloadToken) {
        todayExperience = withContext(Dispatchers.IO) { focusStore.todayExperience() }
    }

    LaunchedEffect(Unit) {
        val recovered = withContext(Dispatchers.IO) { focusStore.restore() }
        experience = withContext(Dispatchers.IO) { focusStore.totalExperience() }
        growthPoints = withContext(Dispatchers.IO) { focusStore.totalPoints() }
        todayExperience = withContext(Dispatchers.IO) { focusStore.todayExperience() }
        focusReport = withContext(Dispatchers.IO) { focusStore.report() }
        recovered.session?.let {
            applySession(it)
            focusRestoreMessage = recovered.pausedByRestart
        }
    }
    LaunchedEffect(goalSettingsVersion) { GoalAlerts.schedule(context.applicationContext) }
    LaunchedEffect(reloadToken, goalSettingsVersion, usageState) {
        if (usageState is UsageLoadState.Ready)
            withContext(Dispatchers.IO) { GoalAlerts.checkNow(context.applicationContext) }
    }
    LaunchedEffect(detail, focusPhase, tab) {
        if (detail == "report" || tab == MainTab.REPORT.name || focusPhase == "result")
            focusReport = withContext(Dispatchers.IO) { focusStore.report() }
    }
    DisposableEffect(lifecycleOwner, focusSessionId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val id = focusSessionId
                if (id != null) scope.launch {
                    withContext(Dispatchers.IO) { focusStore.checkpoint(id) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun goMain(destination: MainTab) {
        detail = ""
        focusGardenOpen = false
        tab = destination.name
    }
    fun openGarden() {
        detail = ""
        focusGardenOpen = true
        tab = MainTab.FOCUS.name
    }
    fun endOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        onboarding = -1
    }
    fun startFocus(minutes: Int) {
        if (startInProgress || focusSessionId != null) return
        startInProgress = true
        scope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { focusStore.start(minutes) }
                applySession(saved)
                completed = false
                resultExperience = 0
                resultPoints = 0
                focusRestoreMessage = false
                stopConfirm = false
                detail = ""
                focusGardenOpen = false
                tab = MainTab.FOCUS.name
            } finally {
                startInProgress = false
            }
        }
    }

    fun finishFocus() {
        val id = focusSessionId ?: return
        if (finishInProgress) return
        finishInProgress = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { focusStore.finish(id) } ?: return@launch
                completed = result.reward != null
                resultExperience = result.reward?.experience ?: 0
                resultPoints = result.reward?.points ?: 0
                elapsedSeconds = result.session.elapsedSeconds
                experience = withContext(Dispatchers.IO) { focusStore.totalExperience() }
                growthPoints = withContext(Dispatchers.IO) { focusStore.totalPoints() }
                todayExperience = withContext(Dispatchers.IO) { focusStore.todayExperience() }
                focusReport = withContext(Dispatchers.IO) { focusStore.report() }
                focusSessionId = null
                focusPhase = "result"
                stopConfirm = false
            } finally {
                finishInProgress = false
            }
        }
    }

    LaunchedEffect(focusPhase, paused, focusSessionId) {
        var lastCheckpoint = -1
        while (focusPhase == "running" && !paused && focusSessionId != null) {
            delay(1000L)
            val now = SystemClock.elapsedRealtime()
            if (focusDuration == 0) {
                elapsedSeconds = ((now - startElapsed) / 1000L).toInt().coerceAtLeast(0)
            } else {
                remainingSeconds = ((endElapsed - now + 999L) / 1000L).toInt().coerceAtLeast(0)
                elapsedSeconds = (focusDuration * 60 - remainingSeconds).coerceAtLeast(0)
                if (remainingSeconds == 0) {
                    paused = true
                    finishFocus()
                    break
                }
            }
            if (elapsedSeconds > 0 && elapsedSeconds % 10 == 0 && lastCheckpoint != elapsedSeconds) {
                lastCheckpoint = elapsedSeconds
                val id = focusSessionId ?: break
                withContext(Dispatchers.IO) { focusStore.checkpoint(id) }
            }
        }
    }

    BackHandler(enabled = onboarding > 0 || detail.isNotEmpty() || focusPhase != "setup" || focusGardenOpen) {
        when {
            onboarding > 0 -> onboarding--
            focusPhase == "running" -> stopConfirm = true
            focusPhase == "result" -> { focusPhase = "setup"; goMain(MainTab.HOME) }
            detail.isNotEmpty() -> detail = ""
            focusGardenOpen -> focusGardenOpen = false
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
                                MainTab.REPORT -> Icons.Outlined.DateRange
                                MainTab.FOCUS -> Icons.Outlined.TrackChanges
                                MainTab.SETTINGS -> Icons.Outlined.Settings
                            }
                            NavigationBarItem(
                                modifier = Modifier.testTag("nav-${item.name}"),
                                selected = restoredMainTab(tab) == item,
                                onClick = { goMain(item) },
                                icon = { Icon(icon, contentDescription = item.label, modifier = Modifier.size(FocusSpacing.icon)) },
                                label = {
                                    val navigationLabel = if (LocalDensity.current.fontScale >= 1.4f && item == MainTab.ANALYSIS) "분석" else item.label
                                    Text(
                                        navigationLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
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
            Box(Modifier.padding(inset).fillMaxSize().semantics { testTagsAsResourceId = true }) {
                when {
                    onboarding == 0 -> OnboardingIntro(
                        onNext = { onboarding = 1 }, onExplore = ::endOnboarding
                    )
                    onboarding == 1 -> OnboardingPermission(
                        permissionGranted = usageAccessGranted,
                        onOpenSettings = { openUsageSettings(context) },
                        onNext = { onboarding = 2 }
                    )
                    onboarding == 2 -> OnboardingGoal(
                        apps = usageSnapshot?.today?.apps?.takeIf { it.isNotEmpty() }
                            ?: usageSnapshot?.last7?.apps.orEmpty(),
                        goals = goals,
                        usageState = usageState,
                        sourceLabel = if (usageSnapshot?.today?.apps.isNullOrEmpty()) "최근 7일 사용한 앱" else "오늘 사용한 앱",
                        onSetGoal = { goalSheetId = it.id },
                        onOpenSettings = { openUsageSettings(context) },
                        onRefresh = { reloadToken++ },
                        onFinish = ::endOnboarding
                    )
                    focusPhase == "running" -> FocusRunningScreen(
                        minutes = focusDuration,
                        seconds = if(focusDuration == 0) elapsedSeconds else remainingSeconds,
                        paused = paused,
                        restoredAfterReboot = focusRestoreMessage,
                        onPauseToggle = {
                            val id = focusSessionId
                            if (id != null) scope.launch {
                                val changed = withContext(Dispatchers.IO) { focusStore.togglePause(id) }
                                if (changed != null) {
                                    applySession(changed)
                                    focusRestoreMessage = false
                                }
                            }
                        },
                        onFinish = { stopConfirm = true }
                    )
                    focusPhase == "result" -> FocusResultScreen(
                        completed = completed,
                        elapsedSeconds = elapsedSeconds,
                        rewardExperience = resultExperience,
                        rewardPoints = resultPoints,
                        onHome = { focusPhase = "setup"; goMain(MainTab.HOME) },
                        onGarden = { focusPhase = "setup"; openGarden() }
                    )
                    detail == "app" && usageSnapshot != null && selectedAppRecord != null -> AppDetailScreen(
                        app = selectedAppRecord!!,
                        snapshot = usageSnapshot,
                        period = selectedAppPeriod,
                        onPeriod = { selectedAppPeriod = it },
                        goalMinutes = goals[selectedAppId],
                        goalDaysMask = goalDays[selectedAppId]
                            ?: GoalDayPolicy.fromLegacyEveryday(everydayGoal[selectedAppId] ?: true),
                        warn80 = warningAt80[selectedAppId] ?: true,
                        warn100 = warningAt100[selectedAppId] ?: true,
                        isDistraction = distraction[selectedAppId] ?: false,
                        isExcluded = excluded[selectedAppId] ?: false,
                        onBack = { detail = "" },
                        onGoal = { goalSheetId = selectedAppId },
                        onSystemSettings = {
                            openInstalledAppInfo(context, selectedAppRecord?.packageName)
                        },
                        onDistraction = { distraction[selectedAppId] = it; prefs.edit().putBoolean("distraction.$selectedAppId", it).apply() },
                        onExcluded = { excluded[selectedAppId] = it; prefs.edit().putBoolean("excluded.$selectedAppId", it).apply() }
                    )
                    detail == "app" -> InfoState("사용 기록 갱신됨", "선택한 앱의 사용 기록을 다시 확인해 주세요.", "사용 분석으로", {detail=""})
                    detail == "report" -> ReportScreen(
                        state = usageState,
                        excluded = excluded,
                        focusSummary = focusReport,
                        onBack = {detail = ""}, onApp = { app, period -> selectedAppId=app.id; selectedAppRecord=app; selectedAppPeriod=period; detail="app" },
                        onUsageSettings = {openUsageSettings(context)}, onRefresh = {reloadToken++}
                    )
                    detail == "permissions" -> key(reloadToken) { PermissionManagementScreen(
                        onBack = {detail = ""},
                        onUsageSettings = {openUsageSettings(context)},
                        onNotificationSettings = {openNotificationSettings(context)}
                    ) }
                    else -> when(restoredMainTab(tab)) {
                        MainTab.HOME -> HomeScreen(
                            state = usageState,
                            goals = goals,
                            excluded = excluded,
                            todayExperience = todayExperience,
                            totalExperience = experience,
                            onSetGoal = { goalSheetId = it.id },
                            onFocus = {goMain(MainTab.FOCUS)},
                            onAnalysis = {goMain(MainTab.ANALYSIS)},
                            onApp = { selectedAppId=it.id;selectedAppRecord=it;selectedAppPeriod="오늘";detail="app" },
                            onGarden = ::openGarden,
                            onReport = {goMain(MainTab.REPORT)},
                            onSettings = {goMain(MainTab.SETTINGS)},
                            onUsageSettings = {openUsageSettings(context)}, onRefresh = {reloadToken++}
                        )
                        MainTab.ANALYSIS -> AnalysisScreen(
                            state = usageState,
                            excluded = excluded,
                            onApp = { app, period -> selectedAppId=app.id; selectedAppRecord=app; selectedAppPeriod=period;detail="app" }, onReport = {goMain(MainTab.REPORT)},
                            onUsageSettings = {openUsageSettings(context)}, onRefresh = {reloadToken++}
                        )
                        MainTab.REPORT -> ReportScreen(
                            state = usageState,
                            excluded = excluded,
                            focusSummary = focusReport,
                            onBack = null,
                            onApp = { app, period ->
                                selectedAppId = app.id
                                selectedAppRecord = app
                                selectedAppPeriod = period
                                detail = "app"
                            },
                            onUsageSettings = { openUsageSettings(context) },
                            onRefresh = { reloadToken++ }
                        )
                        MainTab.FOCUS -> {
                            if (focusGardenOpen) {
                                GardenScreen(
                                    experience = experience,
                                    points = growthPoints,
                                    placedItem = placedGardenItemId,
                                    onItem = { selectedGardenItemId = it.id },
                                    onFocus = { goMain(MainTab.FOCUS) }
                                )
                            } else {
                                FocusSetupScreen(
                                    onStart = ::startFocus,
                                    onGarden = ::openGarden,
                                    selectedDistractions = distraction.values.count { it },
                                    availableApps = (usageSnapshot?.last7?.apps.orEmpty() +
                                        usageSnapshot?.today?.apps.orEmpty()).distinctBy { it.id },
                                    selectedDistractionIds = distraction.filterValues { it }.keys,
                                    onDistractionChange = { id, selected ->
                                        distraction[id] = selected
                                        prefs.edit().putBoolean("distraction.$id", selected).apply()
                                    },
                                    usageAccessGranted = usageAccessGranted
                                )
                            }
                        }
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
            val app = listOfNotNull(usageSnapshot?.today, usageSnapshot?.last7, usageSnapshot?.last30)
                .flatMap { it.apps }.firstOrNull { it.id == goalSheetId }
            if (app != null) GoalSheet(
                app=app,
                current=goals[app.id] ?: 60,
                initialWarn80=warningAt80[app.id] ?: true,
                initialWarn100=warningAt100[app.id] ?: true,
                initialDays=goalDays[app.id]
                    ?: GoalDayPolicy.fromLegacyEveryday(everydayGoal[app.id] ?: true),
                onSave = { minutes, warn80, warn100, days ->
                    val normalizedDays = GoalDayPolicy.normalize(days)
                    goals[app.id] = minutes
                    warningAt80[app.id] = warn80
                    warningAt100[app.id] = warn100
                    goalDays[app.id] = normalizedDays
                    everydayGoal[app.id] = normalizedDays != GoalDayPolicy.NONE
                    prefs.edit()
                        .putInt("goal.${app.id}", minutes)
                        .putBoolean("warn80.${app.id}", warn80)
                        .putBoolean("warn100.${app.id}", warn100)
                        .putInt("goalDays.${app.id}", normalizedDays)
                        .putBoolean("everyday.${app.id}", normalizedDays != GoalDayPolicy.NONE)
                        .apply()
                    goalSettingsVersion++
                    goalSheetId = ""
                    if (normalizedDays != GoalDayPolicy.NONE && (warn80 || warn100) && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onClear = {
                    goals.remove(app.id)
                    warningAt80.remove(app.id)
                    warningAt100.remove(app.id)
                    everydayGoal.remove(app.id)
                    goalDays.remove(app.id)
                    prefs.edit().remove("goal.${app.id}").remove("warn80.${app.id}")
                        .remove("warn100.${app.id}").remove("everyday.${app.id}")
                        .remove("goalDays.${app.id}").apply()
                    goalSettingsVersion++
                    goalSheetId = ""
                },
                onDismiss = {goalSheetId=""}
            )
        }
        if (selectedGardenItemId.isNotEmpty()) {
            val item = GardenCatalog.items.firstOrNull { it.id == selectedGardenItemId }
            if (item != null) GardenItemSheet(
                item = item,
                experience = experience,
                placedItemId = GardenCatalog.placed(placedGardenItemId, experience).id,
                onDismiss = { selectedGardenItemId = "" },
                onSelect = {
                    if (GardenCatalog.canPlace(item.id, experience)) {
                        placedGardenItemId = item.id
                        prefs.edit().putString("placed_garden_item_id", item.id).apply()
                    }
                    selectedGardenItemId = ""
                }
            )
        }
        if(stopConfirm) AlertDialog(
            onDismissRequest = { stopConfirm = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title={Text("집중을 종료할까요?")},
            text = {
                Text(
                    if (com.haebi.focuslock.data.FocusRewardRules.qualifies(focusDuration, elapsedSeconds))
                        "보상 조건을 달성했습니다. 종료하면 기록과 보상이 저장됩니다."
                    else "현재 ${elapsedSeconds / 60}분 ${(elapsedSeconds % 60).toString().padStart(2, '0')}초 진행했습니다. " +
                        "지금 종료하면 보상 없이 기록됩니다."
                )
            },
            confirmButton={TextButton(onClick=::finishFocus){Text("종료")}},
            dismissButton={TextButton(onClick={stopConfirm=false}){Text("계속 집중")}}
        )
        if(showInfo) AlertDialog(
            onDismissRequest={showInfo=false},
            title={Text("FocusLock 구현 상태")},
            text={Text("Android 사용 통계와 앱별 권한 조회, 주기적 목표 경고, Room 집중 기록·보상 저장이 연결되었습니다. 다른 앱 강제 차단은 지원하지 않습니다.")},
            confirmButton={TextButton(onClick={showInfo=false}){Text("확인")}}
        )
    }
}
