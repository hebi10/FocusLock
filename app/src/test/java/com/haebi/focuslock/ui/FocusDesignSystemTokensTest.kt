package com.haebi.focuslock.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class FocusDesignSystemTokensTest {
    @Test
    fun spacingMatchesApprovedDriveDesign() {
        assertEquals(4.dp, FocusSpacing.xxs)
        assertEquals(8.dp, FocusSpacing.xs)
        assertEquals(12.dp, FocusSpacing.sm)
        assertEquals(16.dp, FocusSpacing.md)
        assertEquals(24.dp, FocusSpacing.lg)
        assertEquals(32.dp, FocusSpacing.xl)
        assertEquals(48.dp, FocusSpacing.minTouchTarget)
        assertEquals(52.dp, FocusSpacing.actionHeight)
        assertEquals(24.dp, FocusSpacing.icon)
    }

    @Test
    fun surfacesAndSemanticColorsMatchApprovedDriveDesign() {
        assertEquals(Color(0xFFF7F7F7), FocusColors.BgLight)
        assertEquals(Color(0xFF101010), FocusColors.BgDark)
        assertEquals(Color(0xFF171717), FocusColors.TextLight)
        assertEquals(Color(0xFFF3F3F3), FocusColors.TextDark)
        assertEquals(Color(0xFFE4E4E4), FocusColors.GrayLight)
        assertEquals(Color(0xFF363636), FocusColors.DividerDark)
        assertEquals(Color(0xFF357A57), FocusColors.Green)
        assertEquals(Color(0xFFAA720E), FocusColors.Amber)
        assertEquals(Color(0xFFB84343), FocusColors.Red)
    }

    @Test
    fun neutralBodyTextHasAccessibleContrast() {
        assertTrue(contrast(0xFF171717L, 0xFFF7F7F7L) >= 4.5)
        assertTrue(contrast(0xFF666666L, 0xFFFFFFFFL) >= 4.5)
        assertTrue(contrast(0xFFF3F3F3L, 0xFF101010L) >= 4.5)
        assertTrue(contrast(0xFFA8A8A8L, 0xFF1B1B1BL) >= 4.5)
    }

    private fun contrast(a: Long, b: Long): Double {
        val bright = maxOf(luminance(a), luminance(b))
        val dark = minOf(luminance(a), luminance(b))
        return (bright + 0.05) / (dark + 0.05)
    }

    private fun luminance(color: Long): Double {
        fun channel(shift: Int): Double {
            val normalized = ((color shr shift) and 255).toDouble() / 255
            return if (normalized <= 0.04045) normalized / 12.92
            else ((normalized + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
