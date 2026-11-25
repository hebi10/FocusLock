package com.haebi.focuslock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat

@Composable
internal fun ReferenceUsageSparkline(bars: List<AnalysisBar>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val missing = MaterialTheme.colorScheme.outlineVariant
    val max = bars.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    Canvas(modifier.semantics { contentDescription = "오늘 시간대별 관측 사용량 요약. 자세한 값은 사용 분석에서 확인할 수 있습니다." }) {
        if (bars.isEmpty()) return@Canvas
        val cell = size.width / bars.size
        bars.forEachIndexed { index, bar ->
            val width = cell * 0.58f
            val x = cell * index + (cell - width) / 2f
            val height = if (!bar.observed) 2.dp.toPx()
                else (size.height * bar.minutes / max).coerceAtLeast(if (bar.minutes > 0) 2.dp.toPx() else 0f)
            if (height > 0f) drawRoundRect(
                color = if (bar.observed) accent.copy(alpha = 0.64f) else missing,
                topLeft = Offset(x, size.height - height), size = Size(width, height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
internal fun ReferenceRankRow(app: AppUsage, totalMinutes: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("usage-row").heightIn(min = FocusSpacing.minTouchTarget).clickable(onClick = onClick)
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InstalledAppIcon(app, 32.dp)
        Text(app.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Column(horizontalAlignment = Alignment.End) {
            Text(UsageFormat.minutesLabel(app.minutes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("${UsageFormat.sharePercent(app.minutes, totalMinutes)}%", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ReferenceGoalRing(fraction: Float, label: String) {
    val diameter = if (LocalDensity.current.fontScale >= 1.35f) 70.dp else 94.dp
    val accent = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val area = Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), area, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), area,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** Decorative, non-data artwork inspired by the approved latest concept. */
@Composable
internal fun ReferenceFocusBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF101631), Color(0xFF23375D), FocusColors.NightBackground)))
        fun ridge(color: Color, heights: List<Float>) {
            val path = Path().apply {
                moveTo(0f, size.height)
                heights.forEachIndexed { index, y -> lineTo(size.width * index / (heights.size - 1), size.height * y) }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, color)
        }
        ridge(Color(0xFF253B64), listOf(.53f,.46f,.51f,.40f,.49f,.39f,.50f,.44f,.51f))
        ridge(Color(0xFF1B2B50), listOf(.65f,.57f,.61f,.49f,.63f,.52f,.60f,.54f,.66f))
        ridge(Color(0xFF14203D), listOf(.78f,.66f,.73f,.60f,.75f,.64f,.72f,.66f,.76f))
    }
}

@Composable
internal fun ReferenceFocusDial(duration: Int, onStart: () -> Unit) {
    val large = LocalDensity.current.fontScale >= 1.35f
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, if (large) 182.dp else 212.dp)
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF6E86B9), Color(0xFFA5F2F2), Color(0xFFB2A0F7), Color(0xFF6E86B9))),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (duration == 0) "자유 집중" else "$duration:00", fontSize = if (large || duration == 0) 27.sp else 42.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                FilledIconButton(onClick = onStart, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.PlayArrow, "타이머 시작")
                }
            }
        }
    }
}
