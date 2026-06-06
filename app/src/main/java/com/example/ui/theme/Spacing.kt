package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Spacing tokens for consistent margins, paddings, and alignment grids.
 */
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val doubleExtraLarge: Dp = 48.dp,
    val tripleExtraLarge: Dp = 64.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * Easy reference to M3 Spacing tokens inside Composables:
 * `MaterialTheme.spacing.medium`
 */
val androidx.compose.material3.MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
