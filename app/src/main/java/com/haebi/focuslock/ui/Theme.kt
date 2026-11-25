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
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object FocusColors {
    val Green = Color(0xFF357A57)
    val LightGreen = Color(0xFFE7F1EA)
    val DarkGreen = Color(0xFF9BD5AD)
    val Amber = Color(0xFFAA720E)
    val Red = Color(0xFFB84343)
    val Brand = Color(0xFF6942D9)
    val BrandDark = Color(0xFFBBA8FF)
    val BrandTint = Color(0xFFEEE8FF)
    val BrandTintDark = Color(0xFF352B50)
    val NightBackground = Color(0xFF10182F)
    val NightSurface = Color(0xFF202B49)
    val BgLight = Color(0xFFF6F5FB)
    val BgDark = Color(0xFF101010)
    val GrayLight = Color(0xFFE5E3EF)
    val TextLight = Color(0xFF171717)
    val TextDark = Color(0xFFF3F3F3)
    val SecondaryTextLight = Color(0xFF646276)
    val SecondaryTextDark = Color(0xFFA8A8A8)
    val SurfaceLight = Color.White
    val SurfaceDark = Color(0xFF1B1B1B)
    val SurfaceVariantLight = Color(0xFFF0EEF8)
    val SurfaceVariantDark = Color(0xFF2D2D2D)
    val DividerDark = Color(0xFF363636)
}

object FocusSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val minTouchTarget = 48.dp
    val actionHeight = 52.dp
    val icon = 24.dp
}

private val focusTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

private val lightScheme = lightColorScheme(
    primary = FocusColors.Brand,
    onPrimary = FocusColors.SurfaceLight,
    primaryContainer = FocusColors.BrandTint,
    onPrimaryContainer = Color(0xFF3F258C),
    background = FocusColors.BgLight,
    onBackground = FocusColors.TextLight,
    surface = FocusColors.SurfaceLight,
    onSurface = FocusColors.TextLight,
    surfaceVariant = FocusColors.SurfaceVariantLight,
    onSurfaceVariant = FocusColors.SecondaryTextLight,
    outline = Color(0xFF827D92),
    outlineVariant = FocusColors.GrayLight,
    secondary = FocusColors.Green,
    onSecondary = Color.White,
    error = FocusColors.Red
)

private val darkScheme = darkColorScheme(
    primary = FocusColors.BrandDark,
    onPrimary = Color(0xFF21143D),
    primaryContainer = FocusColors.BrandTintDark,
    onPrimaryContainer = Color(0xFFE8DFFF),
    background = FocusColors.BgDark,
    onBackground = FocusColors.TextDark,
    surface = FocusColors.SurfaceDark,
    onSurface = FocusColors.TextDark,
    surfaceVariant = FocusColors.SurfaceVariantDark,
    onSurfaceVariant = FocusColors.SecondaryTextDark,
    outline = Color(0xFF8E899E),
    outlineVariant = FocusColors.DividerDark,
    secondary = FocusColors.DarkGreen,
    onSecondary = Color(0xFF101010),
    error = Color(0xFFF2A3A3)
)

private val focusNightScheme = darkScheme.copy(
    background = FocusColors.NightBackground,
    surface = FocusColors.NightSurface,
    surfaceVariant = Color(0xFF263553),
    onSurface = Color(0xFFF6F5FC),
    onBackground = Color(0xFFF6F5FC),
    onSurfaceVariant = Color(0xFFC3CBE2),
    outline = Color(0xFF8994B4),
    outlineVariant = Color(0xFF3A4869),
    primaryContainer = Color(0xFF383363),
    onPrimaryContainer = Color(0xFFE6DEFF)
)

@Composable
fun FocusLockTheme(themeChoice: String = "시스템", nightFocus: Boolean = false, content: @Composable () -> Unit) {
    val dark = nightFocus || when (themeChoice) {
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
        colorScheme = if (nightFocus) focusNightScheme else if (dark) darkScheme else lightScheme,
        typography = focusTypography,
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
