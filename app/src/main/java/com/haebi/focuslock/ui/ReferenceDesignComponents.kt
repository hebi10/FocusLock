package com.haebi.focuslock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
        // Static decorative skyline; never used as a usage or progress indicator.
        listOf(.07f to .16f, .22f to .10f, .48f to .19f, .76f to .12f, .9f to .22f).forEach { (x, y) ->
            drawCircle(Color(0xFFBAC6E8).copy(alpha = .45f), 1.dp.toPx(), Offset(size.width * x, size.height * y))
        }
        listOf(.04f,.12f,.21f,.34f,.47f,.6f,.73f,.87f,.96f).forEachIndexed { index, x ->
            val y = size.height * (if (index % 2 == 0) .80f else .84f)
            drawCircle(Color(0xFF14203D), size.width * .065f, Offset(size.width * x, y))
        }
    }
}

@Composable
internal fun ReferenceFocusDial(duration: Int, onStart: () -> Unit) {
    val large = LocalDensity.current.fontScale >= 1.35f
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, if (large) 204.dp else 252.dp)
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
                Text(if (duration == 0) "자유 집중" else "$duration:00", fontSize = if (large || duration == 0) 27.sp else 49.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                FilledIconButton(onClick = onStart, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.PlayArrow, "타이머 시작")
                }
            }
        }
    }
}

/** Keep the essential qualifier visible; expose the full original measurement wording on demand. */
@Composable
internal fun ReferenceMeasurementNote(summary: String, detail: String, tag: String) {
    var expanded by rememberSaveable(tag) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(summary, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) {
                Text(if (expanded) "접기" else "집계 기준", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (expanded) Text(detail, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Zero is an observed value; null splits the line and must never be interpolated. */
internal fun observedLineSegments(values: List<Int?>): List<List<Int>> {
    val segments = mutableListOf<MutableList<Int>>()
    var current: MutableList<Int>? = null
    values.forEachIndexed { index, value ->
        if (value == null) current = null
        else {
            val run = current ?: mutableListOf<Int>().also { segments.add(it); current = it }
            run.add(index)
        }
    }
    return segments
}

/** Full-window overview, separate from the accessible 48dp selection controls. */
@Composable
internal fun ReferenceObservedLine(values: List<Int?>, selected: Int, tag: String, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val max = values.filterNotNull().maxOrNull()?.coerceAtLeast(1) ?: 1
    val segments = observedLineSegments(values)
    Canvas(modifier.fillMaxWidth().height(112.dp).testTag(tag).semantics {
        contentDescription = "관측 사용량 전체 그래프. 기록 미확인 구간은 연결하지 않습니다. 선택 값은 별도 조작 영역에서 확인할 수 있습니다."
    }) {
        val pad = 5.dp.toPx()
        val baseline = size.height - pad
        val plotHeight = (size.height - pad * 2).coerceAtLeast(1f)
        fun x(i: Int) = pad + (size.width - pad * 2) * i / (values.size - 1).coerceAtLeast(1)
        fun point(i: Int) = Offset(x(i), baseline - plotHeight * (values[i] ?: 0).coerceAtLeast(0) / max)
        for (i in 0..4) {
            val y = pad + plotHeight * i / 4f
            drawLine(grid, Offset(pad, y), Offset(size.width - pad, y), .5.dp.toPx())
        }
        values.forEachIndexed { i, value ->
            if (value == null) {
                drawLine(grid, Offset(x(i) - 2.dp.toPx(), baseline), Offset(x(i) + 2.dp.toPx(), baseline), 2.dp.toPx())
            }
        }
        segments.forEach { run ->
            if (run.size >= 2) {
                val area = Path().apply {
                    moveTo(x(run.first()), baseline)
                    run.forEach { i -> val p = point(i); lineTo(p.x, p.y) }
                    lineTo(x(run.last()), baseline)
                    close()
                }
                drawPath(area, brush = Brush.verticalGradient(listOf(accent.copy(alpha = .24f), accent.copy(alpha = .02f))))
            }
            run.zipWithNext().forEach { (a, b) ->
                drawLine(accent, point(a), point(b), 2.dp.toPx(), StrokeCap.Round)
            }
            run.forEach { i -> drawCircle(accent, if (i == selected) 4.dp.toPx() else 2.6.dp.toPx(), point(i)) }
        }
    }
}
