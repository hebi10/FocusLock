package com.haebi.focuslock.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
internal fun LocalDataScreen(
    busy: Boolean,
    message: String,
    onBack: () -> Unit,
    onExport: (Uri) -> Unit,
    onClearGoalHistory: () -> Unit,
    onClearFocusHistory: () -> Unit
) {
    var confirmation by rememberSaveable { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onExport(uri)
    }
    AdaptiveScrollColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.Outlined.ArrowBack, "뒤로") }
            Text("데이터 관리", style = MaterialTheme.typography.headlineMedium)
        }
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("처리 중입니다.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("local-data-result"))
        FocusCard {
            SectionTitle("기록 내보내기")
            Text("현재 목표·목표 이력·종료된 집중 기록과 연결 보상·정원 배치를 JSON 파일로 저장합니다.",
                style = MaterialTheme.typography.bodyMedium)
            Text("Android 시스템 사용 기록과 진행 중인 세션은 포함하지 않습니다. 파일에는 앱 이름과 사용 목표 등 개인 정보가 포함될 수 있으므로 저장 위치와 공유 여부를 직접 선택해 주세요. 가져오기 기능은 제공하지 않습니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionButton("JSON 내보내기", { export.launch("FocusLock-records-${LocalDate.now()}.json") },
                modifier = Modifier.testTag("local-data-export"), enabled = !busy)
        }
        FocusCard {
            SectionTitle("목표 이력")
            Text("과거 목표 변경 이력과 해당 이력에 근거한 달성률을 초기화합니다. 현재 목표 시간, 요일, 경고 알림 설정은 유지되며 오늘부터 다시 기록합니다.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirmation = "goals" }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget)
                    .testTag("local-data-clear-goals")) {
                Text("목표 이력 삭제", color = MaterialTheme.colorScheme.error)
            }
        }
        FocusCard {
            SectionTitle("집중 기록 및 보상")
            Text("종료·중단된 집중 세션과 연결된 경험치·포인트를 삭제합니다. 정원 성장 및 해금 상태에도 영향을 주며 사용할 수 없는 배치는 기본 아이템으로 돌아갑니다. 진행 중인 세션이 있으면 삭제하지 않습니다.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirmation = "focus" }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget)
                    .testTag("local-data-clear-focus")) {
                Text("집중 기록 및 보상 삭제", color = MaterialTheme.colorScheme.error)
            }
        }
        Text("이 기능은 FocusLock 내부 기록만 관리합니다. Android가 보관하는 앱 사용 기록과 이미 내보낸 파일은 별도로 관리해야 합니다.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (confirmation.isNotEmpty()) {
        val goals = confirmation == "goals"
        AlertDialog(
            onDismissRequest = { confirmation = "" },
            title = { Text(if (goals) "목표 이력을 삭제할까요?" else "집중 기록 및 보상을 삭제할까요?") },
            text = {
                Text(if (goals) "과거 목표 이력은 복구할 수 없습니다. 현재 목표 설정은 유지됩니다."
                    else "종료된 집중 기록과 연결 보상은 복구할 수 없습니다. 정원 성장 상태도 변경됩니다.")
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    confirmation = ""
                    if (goals) onClearGoalHistory() else onClearFocusHistory()
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmation = "" }) { Text("취소") } }
        )
    }
}
