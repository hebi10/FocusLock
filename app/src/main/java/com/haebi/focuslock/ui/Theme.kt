package com.haebi.focuslock.ui

import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape

object FocusColors {
    val Green = Color(0xFF357A57)
    val LightGreen = Color(0xFFE7F1EA)
    val DarkGreen = Color(0xFF9BD5AD)
    val Amber = Color(0xFFAA720E)
    val Red = Color(0xFFB84343)
    val BgLight = Color(0xFFF7F7F7)
    val BgDark = Color(0xFF101010)
    val GrayLight = Color(0xFFE4E4E4)
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color.White,
    background = FocusColors.BgLight,
    onBackground = Color(0xFF171717),
    surface = Color.White,
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF666666),
    outlineVariant = FocusColors.GrayLight,
    secondary = FocusColors.Green,
    onSecondary = Color.White,
    error = FocusColors.Red,
    surfaceDim = Color(0xFFDADADA),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = FocusColors.BgLight,
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = FocusColors.GrayLight,
    surfaceTint = Color(0xFF171717),
    primaryContainer = Color(0xFFEEEEEE),
    onPrimaryContainer = Color(0xFF171717),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF666666),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF171717),
    inverseSurface = Color(0xFF2D2D2D),
    inverseOnSurface = Color(0xFFF3F3F3),
    inversePrimary = Color(0xFFF3F3F3),
    outline = Color(0xFF767676),
    onError = Color.White,
    errorContainer = Color(0xFFFBE8E8),
    onErrorContainer = Color(0xFF711E1E)
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFF3F3F3),
    onPrimary = Color(0xFF171717),
    background = FocusColors.BgDark,
    onBackground = Color(0xFFF3F3F3),
    surface = Color(0xFF1B1B1B),
    onSurface = Color(0xFFF3F3F3),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB7B7B7),
    outlineVariant = Color(0xFF363636),
    secondary = FocusColors.DarkGreen,
    onSecondary = Color(0xFF101010),
    error = Color(0xFFF2A3A3),
    surfaceDim = FocusColors.BgDark,
    surfaceBright = Color(0xFF363636),
    surfaceContainerLowest = FocusColors.BgDark,
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF1B1B1B),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2D2D2D),
    surfaceTint = Color(0xFFF3F3F3),
    primaryContainer = Color(0xFF2D2D2D),
    onPrimaryContainer = Color(0xFFF3F3F3),
    secondaryContainer = Color(0xFF2D2D2D),
    onSecondaryContainer = Color(0xFFF3F3F3),
    tertiary = Color(0xFFB7B7B7),
    onTertiary = Color(0xFF171717),
    tertiaryContainer = Color(0xFF2D2D2D),
    onTertiaryContainer = Color(0xFFF3F3F3),
    inverseSurface = Color(0xFFEEEEEE),
    inverseOnSurface = Color(0xFF171717),
    inversePrimary = Color(0xFF171717),
    outline = Color(0xFF8A8A8A),
    onError = Color(0xFF3C1515),
    errorContainer = Color(0xFF4A2424),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun FocusLockTheme(themeChoice: String = "시스템", content: @Composable () -> Unit) {
    val dark = when (themeChoice) {
        "다크" -> true
        "라이트" -> false
        else -> isSystemInDarkTheme()
    }
    val activity = LocalActivity.current
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= 30) {
            val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else flags, flags)
        } else {
            val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility =
                (window.decorView.systemUiVisibility and flags.inv()) or (if (dark) 0 else flags)
        }
    }
    MaterialTheme(
        colorScheme = if (dark) darkScheme else lightScheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
