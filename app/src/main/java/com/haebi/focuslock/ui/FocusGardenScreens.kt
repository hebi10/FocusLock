package com.haebi.focuslock.ui

import androidx.compose.foundation.Image
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
        ActionButton("집중 시작", { onStart(duration) })
    }
    if (showDistractionSheet) {
        ModalBottomSheet(onDismissRequest = { showDistractionSheet = false }) {
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
private val gardenItems = listOf(
    GardenItem("sprout", "작은 새싹", "🌱", 0),
    GardenItem("flowers", "꽃밭", "🌼", 60),
    GardenItem("tree", "작은 나무", "🌳", 150),
    GardenItem("bench", "정원 벤치", "🪑", 300)
)

@Composable
fun GardenScreen(experience: Int, placedItem: String, onItem: (GardenItem) -> Unit, onFocus: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle("나의 정원", "집중 성과가 쌓일수록 성장합니다")
            FocusCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("레벨 ${experience / 100 + 1}", fontWeight = FontWeight.Bold)
                    Text("경험치 ${experience % 100} / 100 · 누적 ${experience}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProgressBar((experience % 100) / 100f)
            }
            if(wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GardenScene(placedItem, Modifier.weight(1f))
                    GardenItems(experience, onItem, Modifier.weight(1f))
                }
            } else {
                GardenScene(placedItem)
                GardenItems(experience, onItem)
            }
            ActionButton("집중 시작", onFocus)
        }
    }
}

@Composable
private fun GardenScene(placedItem: String, modifier: Modifier = Modifier) {
    FocusCard(modifier) {
        SectionTitle("작은 정원")
        Image(
            painterResource(R.drawable.garden_illustration),
            contentDescription = "모자를 쓴 캐릭터와 새싹이 있는 정원",
            modifier = Modifier.fillMaxWidth().height(190.dp)
        )
        Text("배치된 아이템: $placedItem", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("오늘의 집중으로 조금씩 자라는 공간", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun GardenItems(experience: Int, onItem: (GardenItem) -> Unit, modifier: Modifier = Modifier) {
    FocusCard(modifier) {
        SectionTitle("아이템")
        gardenItems.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { item ->
                    val unlocked = experience >= item.requirement
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onItem(item) },
                        shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.symbol, fontSize = 28.sp)
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(if (unlocked) "보유" else "${item.requirement} XP 필요", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if(pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenItemSheet(item: GardenItem, experience: Int, onDismiss: () -> Unit, onSelect: () -> Unit) {
    val unlocked = experience >= item.requirement
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.symbol, fontSize = 56.sp)
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(if(unlocked) "사용 가능한 정원 아이템" else "${item.requirement} 경험치를 모으면 해금됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionButton(if(unlocked) "정원에 배치" else "해금 전", onClick = onSelect, enabled = unlocked)
        }
    }
}
