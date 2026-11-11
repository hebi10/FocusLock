package com.haebi.focuslock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.R
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.DemoData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSetupScreen(
    onStart: (Int) -> Unit,
    selectedDistractions: Int,
    availableApps: List<AppUsage>,
    selectedDistractionIds: Set<String>,
    onDistractionChange: (String, Boolean) -> Unit,
    usageAccessGranted: Boolean
) {
    var mode by rememberSaveable { mutableStateOf("25분") }
    var customMinutes by rememberSaveable { mutableIntStateOf(45) }
    var showDistractionSheet by rememberSaveable { mutableStateOf(false) }
    val duration = when (mode) {
        "10분" -> 10
        "25분" -> 25
        "사용자" -> customMinutes
        else -> 0
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        PageTitle("집중", "지금 할 일에 집중할 시간을 정하세요")
        FocusCard {
            SectionTitle("집중 모드")
            PeriodSelector(listOf("10분", "25분", "사용자", "자유"), mode) { mode = it }
            if (mode == "사용자") {
                Slider(
                    value = customMinutes.toFloat(),
                    onValueChange = { customMinutes = (it / 5).toInt().coerceIn(1, 18) * 5 },
                    valueRange = 5f..90f
                )
                Text("사용자 지정 · ${customMinutes}분", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (duration == 0) "자유 집중" else DemoData.minutesLabel(duration),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                if (duration == 0) "자유 집중은 10분 이상 기록하면 완료 보상을 받습니다."
                else "설정한 집중 시간을 채우면 완료 보상을 받습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        FocusCard {
            SectionTitle("집중 중 사용 기록")
            Text("방해 앱 ${selectedDistractions}개 선택됨", fontWeight = FontWeight.Medium)
            Text(
                "선택한 앱은 관리 대상으로 저장됩니다. 실제 방해 앱 사용 감지나 강제 차단은 지원하지 않습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            when {
                !usageAccessGranted -> Text(
                    "사용 정보 접근 권한이 없어 앱 목록을 볼 수 없습니다. 집중 타이머는 이용할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall
                )
                availableApps.isEmpty() -> Text(
                    "실제 사용 기록이 있는 앱이 없습니다. 기록 수집 후 선택할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = { showDistractionSheet = true },
                enabled = usageAccessGranted && availableApps.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("방해 앱 선택") }
        }
        FocusCard {
            SectionTitle("집중 후 보상")
            Text("완료 기록과 보상은 기기에 저장됩니다.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "실제 완료 세션당 한 번만 경험치와 성장 포인트가 지급됩니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            ActionButton("집중 시작", { onStart(duration) })
        }
    }
    if (showDistractionSheet) {
        ModalBottomSheet(onDismissRequest = { showDistractionSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle("방해 앱 선택")
                Text(
                    "실제 사용 기록이 있는 앱만 표시합니다. 선택해도 앱이 자동 차단되지 않습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                availableApps.distinctBy { it.id }.forEach { app ->
                    val selected = app.id in selectedDistractionIds
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable { onDistractionChange(app.id, !selected) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(app.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked = selected, onCheckedChange = null)
                    }
                }
                TextButton(
                    onClick = { showDistractionSheet = false },
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                ) { Text("완료") }
            }
        }
    }
}

@Composable
fun FocusRunningScreen(minutes: Int, seconds: Int, paused: Boolean, restoredAfterReboot: Boolean, onPauseToggle: () -> Unit, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (paused) "일시정지" else "집중 중", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if(minutes == 0) "자유 집중" else "${minutes}분 집중", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val clock = "%02d:%02d".format(seconds / 60, seconds % 60)
            Text(clock, fontSize = 69.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold)
            if (minutes > 0) ProgressBar(1f - seconds.toFloat() / (minutes * 60).coerceAtLeast(1), neutral = true)
            Text(
                when {
                    restoredAfterReboot -> "기기 재시작으로 일시정지되었습니다. 계속 집중을 누르면 이어집니다."
                    paused -> "집중 시간을 일시정지했습니다."
                    else -> "진행 상태는 저장되며 앱을 다시 열면 기록을 복구합니다."
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPauseToggle, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(if (paused) "계속 집중" else "일시정지")
            }
            ActionButton(if (minutes == 0) "집중 완료" else "집중 종료", onFinish)
        }
    }
}

@Composable
fun FocusResultScreen(
    completed: Boolean,
    elapsedSeconds: Int,
    rewardExperience: Int,
    rewardPoints: Int,
    onHome: () -> Unit,
    onGarden: () -> Unit
) {
    val earned = rewardExperience > 0 || rewardPoints > 0
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(if (completed) "집중 완료" else "집중 기록", style = MaterialTheme.typography.headlineMedium)
        Image(
            painterResource(R.drawable.garden_illustration),
            contentDescription = "정원 일러스트",
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )
        Text(
            "${elapsedSeconds / 60}분 ${(elapsedSeconds % 60).toString().padStart(2, '0')}초",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            if (completed) "설정한 집중 시간을 달성했습니다." else "설정 시간 전에 종료한 집중 기록입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FocusCard {
            SectionTitle("이번 세션 보상")
            Text(
                if (earned) "경험치 +${rewardExperience} · 성장 포인트 +${rewardPoints}"
                else "완료 조건 미충족 · 지급된 보상 없음",
                color = if (earned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "완료·중단 기록은 기기에 저장되며 보상은 세션당 한 번만 지급됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        ActionButton("홈으로", onHome)
        TextButton(onClick = onGarden, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("정원 보기")
        }
    }
}

data class GardenItem(val id: String, val name: String, val symbol: String, val requirement: Int)

internal object GardenCatalog {
    val items = listOf(
        GardenItem("sprout", "작은 새싹", "🌱", 0),
        GardenItem("flowers", "꽃밭", "🌼", 60),
        GardenItem("tree", "작은 나무", "🌳", 150),
        GardenItem("bench", "정원 벤치", "🪑", 300)
    )

    fun fromSaved(value: String?): GardenItem =
        items.firstOrNull { it.id == value || it.name == value } ?: items.first()

    fun canPlace(id: String, experience: Int): Boolean =
        items.any { it.id == id && experience >= it.requirement }

    fun placed(value: String?, experience: Int): GardenItem =
        fromSaved(value).takeIf { canPlace(it.id, experience) } ?: items.first()
}

@Composable
fun GardenScreen(
    experience: Int,
    points: Int,
    placedItem: String,
    onItem: (GardenItem) -> Unit,
    onFocus: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        val next = GardenCatalog.items.firstOrNull { experience < it.requirement }
        val placed = GardenCatalog.placed(placedItem, experience)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageTitle("나의 정원", "집중 성과를 모아 나만의 정원을 가꿔보세요")
            FocusCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("레벨 ${experience / 100 + 1}", fontWeight = FontWeight.Bold)
                    Text("경험치 ${experience % 100} / 100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProgressBar((experience % 100) / 100f)
                Text(
                    "누적 ${experience} XP · 성장 포인트 ${points}P",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (next == null) "모든 정원 아이템을 해금했습니다."
                    else "다음 해금: ${next.name} · ${next.requirement - experience} XP 남음",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GardenScene(placed, Modifier.weight(1f))
                    GardenItems(experience, placed.id, onItem, Modifier.weight(1f))
                }
            } else {
                GardenScene(placed)
                GardenItems(experience, placed.id, onItem)
            }
            ActionButton("집중 시작", onFocus)
        }
    }
}

@Composable
private fun GardenScene(item: GardenItem, modifier: Modifier = Modifier) {
    FocusCard(modifier) {
        SectionTitle("작은 정원")
        Box(Modifier.fillMaxWidth().height(195.dp)) {
            Image(
                painterResource(R.drawable.garden_illustration),
                contentDescription = "기본 정원 배경",
                modifier = Modifier.fillMaxSize()
            )
            GardenDecoration(
                item.id,
                Modifier.align(Alignment.BottomEnd).padding(12.dp).size(88.dp)
                    .semantics { contentDescription = "현재 배치: ${item.name}" }
            )
        }
        Text("배치 중 · ${item.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "아이템을 선택하면 정원 장식이 변경되며, 배치로 포인트가 차감되지 않습니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun GardenDecoration(itemId: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val leaf = Color(0xFF6E8D78)
        val pale = Color(0xFFB5C6B7)
        val brown = Color(0xFF89705C)
        val gold = Color(0xFFD3B26C)
        when (itemId) {
            "sprout" -> {
                drawLine(leaf, Offset(w * .5f, h * .9f), Offset(w * .5f, h * .35f), w * .065f, cap = StrokeCap.Round)
                drawOval(leaf, Offset(w * .15f, h * .28f), Size(w * .37f, h * .25f))
                drawOval(pale, Offset(w * .49f, h * .16f), Size(w * .38f, h * .25f))
            }
            "flowers" -> {
                listOf(.25f, .5f, .75f).forEachIndexed { index, x ->
                    val flowerY = if (index == 1) .26f else .42f
                    drawLine(leaf, Offset(w * x, h * .9f), Offset(w * x, h * flowerY), w * .035f)
                    listOf(
                        Offset(-.11f, 0f), Offset(.11f, 0f),
                        Offset(0f, -.11f), Offset(0f, .11f)
                    ).forEach { offset ->
                        drawCircle(pale, w * .105f, Offset(w * (x + offset.x), h * (flowerY + offset.y)))
                    }
                    drawCircle(gold, w * .075f, Offset(w * x, h * flowerY))
                }
            }
            "tree" -> {
                drawRoundRect(brown, Offset(w * .43f, h * .48f), Size(w * .14f, h * .45f), CornerRadius(w * .04f))
                drawCircle(leaf, w * .29f, Offset(w * .5f, h * .34f))
                drawCircle(pale, w * .20f, Offset(w * .34f, h * .40f))
                drawCircle(leaf, w * .21f, Offset(w * .67f, h * .42f))
            }
            "bench" -> {
                drawRoundRect(brown, Offset(w * .15f, h * .38f), Size(w * .70f, h * .24f), CornerRadius(w * .035f))
                drawRoundRect(gold, Offset(w * .12f, h * .65f), Size(w * .76f, h * .10f), CornerRadius(w * .025f))
                drawRect(brown, Offset(w * .23f, h * .74f), Size(w * .09f, h * .19f))
                drawRect(brown, Offset(w * .68f, h * .74f), Size(w * .09f, h * .19f))
            }
        }
    }
}

@Composable
private fun GardenItems(
    experience: Int,
    placedItemId: String,
    onItem: (GardenItem) -> Unit,
    modifier: Modifier = Modifier
) {
    FocusCard(modifier) {
        SectionTitle("아이템")
        GardenCatalog.items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { item ->
                    val unlocked = GardenCatalog.canPlace(item.id, experience)
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onItem(item) }
                            .testTag("garden-item-${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GardenDecoration(item.id, Modifier.size(44.dp))
                            Text(item.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(
                                when {
                                    placedItemId == item.id -> "배치 중"
                                    unlocked -> "배치 가능"
                                    else -> "${item.requirement - experience} XP 남음"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenItemSheet(
    item: GardenItem,
    experience: Int,
    placedItemId: String,
    onDismiss: () -> Unit,
    onSelect: () -> Unit
) {
    val unlocked = GardenCatalog.canPlace(item.id, experience)
    val placed = placedItemId == item.id
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GardenDecoration(item.id, Modifier.size(72.dp))
            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            Text(
                when {
                    placed -> "현재 정원에 배치된 아이템입니다."
                    unlocked -> "집중으로 해금된 아이템입니다. 포인트 차감 없이 배치할 수 있습니다."
                    else -> "${item.requirement - experience} XP를 더 모으면 해금됩니다."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "해금 기준 ${item.requirement} XP · 현재 ${experience} XP",
                style = MaterialTheme.typography.bodySmall
            )
            ActionButton(
                when {
                    placed -> "배치 중"
                    unlocked -> "정원에 배치"
                    else -> "해금 전"
                },
                onClick = onSelect,
                enabled = unlocked && !placed
            )
        }
    }
}
