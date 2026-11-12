package com.haebi.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageFormat

@Composable
fun PageTitle(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(FocusSpacing.xxs))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        action?.invoke()
    }
}

@Composable
fun SectionTitle(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Outlined.ArrowForwardIos, null, Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun FocusCard(modifier: Modifier = Modifier, contentPadding: Dp = FocusSpacing.md, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm), content = content)
    }
}

@Composable
fun Metric(value: String, hint: String, note: String? = null) {
    Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Text(value, style = MaterialTheme.typography.displaySmall)
    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ProgressBar(fraction: Float, isWarning: Boolean = false, neutral: Boolean = false) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = when {
            isWarning -> FocusColors.Amber
            neutral -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        },
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(7.dp)
    )
}

@Composable
fun AppSymbol(app: AppUsage, size: Dp = 38.dp) {
    Box(
        modifier = Modifier.size(size).background(Color(app.tint).copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(app.symbol, color = Color(app.tint), fontWeight = FontWeight.Black, fontSize = if (size < 40.dp) 20.sp else 24.sp)
    }
}

@Composable
fun AppUsageRow(app: AppUsage, goal: Int? = null, totalMinutes: Int = 0, onClick: () -> Unit) {
    val exceed = goal != null && app.minutes > goal
    Row(
        modifier = Modifier.fillMaxWidth().testTag("usage-row").heightIn(min = FocusSpacing.minTouchTarget).clickable(onClick = onClick).padding(vertical = FocusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSymbol(app)
        Column(Modifier.weight(1f)) {
            Text(app.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${if (app.launches < 0) "실행 횟수 확인 불가" else "실행 ${app.launches}회"} · 마지막 사용 ${app.lastUsed}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(UsageFormat.minutesLabel(app.minutes), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${UsageFormat.sharePercent(app.minutes, totalMinutes)}%", color = if (exceed) FocusColors.Red else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ArrowForwardIos, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CategoryRow(label: String, minutes: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Text(UsageFormat.minutesLabel(minutes), fontSize = 12.sp, modifier = Modifier.width(48.dp))
        Box(Modifier.weight(1f)) { ProgressBar(if (total == 0) 0f else minutes.toFloat() / total, neutral = true) }
        Text("${UsageFormat.sharePercent(minutes, total)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
    }
}

@Composable
fun PeriodSelector(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    val largeFontFourOptions = options.size >= 4 && LocalDensity.current.fontScale >= 1.35f
    val optionRows = if (largeFontFourOptions) options.chunked(2) else listOf(options)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        optionRows.forEach { rowOptions ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                rowOptions.forEach { option ->
                    val active = selected == option
                    Box(
                        Modifier.weight(1f).heightIn(min = FocusSpacing.minTouchTarget)
                            .testTag("period-option-$option")
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.small)
                            .clickable { onSelected(option) }.padding(vertical = FocusSpacing.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>, maxValue: Int, highlight: Int = values.lastIndex, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, value ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height((136f * (value.toFloat() / maxValue.coerceAtLeast(1))).coerceAtLeast(if (value > 0) 3f else 0f).dp)
                            .background(if (i == highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            labels.forEach { label ->
                Text(label, fontSize = if (labels.size > 8) 9.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = FocusSpacing.actionHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SettingRow(title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = FocusSpacing.minTouchTarget).clickable(onClick = onClick).padding(vertical = FocusSpacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
        if (value != null) Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Outlined.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
    }
}

@Composable
fun InfoState(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    FocusCard {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) ActionButton(action, onAction)
    }
}
