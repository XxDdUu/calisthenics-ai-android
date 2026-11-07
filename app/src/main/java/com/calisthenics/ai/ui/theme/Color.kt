package com.calisthenics.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val card: Color,
    val input: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val error: Color,
    val surfaceHover: Color,
    val surfaceActive: Color,
    val mutedText: Color,
    val disabledText: Color,
    val subtleBorder: Color,
    val strongBorder: Color,
    val accentHover: Color,
    val warning: Color,
    val info: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF171717),
    surfaceVariant = Color(0xFF202020),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFA0A0A0),
    border = Color(0xFF303030),
    card = Color(0xFF171717),
    input = Color(0xFF202020),
    accent = Color(0xFFB7F56A),
    onAccent = Color(0xFF0D0D0D),
    success = Color(0xFFB7F56A),
    error = Color(0xFFF87171),
    surfaceHover = Color(0xFF262626),
    surfaceActive = Color(0xFF303030),
    mutedText = Color(0xFF777777),
    disabledText = Color(0xFF555555),
    subtleBorder = Color(0xFF262626),
    strongBorder = Color(0xFF404040),
    accentHover = Color(0xFFC6FF87),
    warning = Color(0xFFFFD166),
    info = Color(0xFF64B5F6),
    isDark = true
)

val DarkV2AppColors = AppColors(
    background = Color(0xFF181A1B),
    surface = Color(0xFF202324),
    surfaceVariant = Color(0xFF272B2C),
    primaryText = Color(0xFFE2E4E3),
    secondaryText = Color(0xFFA6ADAA),
    border = Color(0xFF343A3B),
    card = Color(0xFF202324),
    input = Color(0xFF272B2C),
    accent = Color(0xFF8FA9A1),
    onAccent = Color(0xFF181A1B),
    success = Color(0xFF8EAD97),
    error = Color(0xFFC58F8F),
    surfaceHover = Color(0xFF2D3233),
    surfaceActive = Color(0xFF343A3B),
    mutedText = Color(0xFF777F7C),
    disabledText = Color(0xFF555D5A),
    subtleBorder = Color(0xFF2B3031),
    strongBorder = Color(0xFF454C4D),
    accentHover = Color(0xFFA2B9B0),
    warning = Color(0xFFC5AD7D),
    info = Color(0xFF8FA6B8),
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F5),
    primaryText = Color(0xFF171717),
    secondaryText = Color(0xFF666666),
    border = Color(0xFFE5E7EB),
    card = Color(0xFFFFFFFF),
    input = Color(0xFFF5F5F5),
    accent = Color(0xFF16A34A),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC2626),
    surfaceHover = Color(0xFFF1F3F5),
    surfaceActive = Color(0xFFE5E7EB),
    mutedText = Color(0xFF666666),
    disabledText = Color(0xFF9CA3AF),
    subtleBorder = Color(0xFFE5E7EB),
    strongBorder = Color(0xFFD1D5DB),
    accentHover = Color(0xFF15803D),
    warning = Color(0xFFFFD700),
    info = Color(0xFF2563EB),
    isDark = false
)

val NeutralAppColors = AppColors(
    background = Color(0xFFE5E7EB),
    surface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFFD1D5DB),
    primaryText = Color(0xFF292929),
    secondaryText = Color(0xFF666666),
    border = Color(0xFFC4C7CC),
    card = Color(0xFFF3F4F6),
    input = Color(0xFFE5E7EB),
    accent = Color(0xFF2E7D32),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC2626),
    surfaceHover = Color(0xFFE5E7EB),
    surfaceActive = Color(0xFFD1D5DB),
    mutedText = Color(0xFF666666),
    disabledText = Color(0xFF9CA3AF),
    subtleBorder = Color(0xFFD1D5DB),
    strongBorder = Color(0xFFC4C7CC),
    accentHover = Color(0xFF256B2A),
    warning = Color(0xFFFFD700),
    info = Color(0xFF2563EB),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

val Ink: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
val Surface: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface
val Elevated: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceVariant
val PrimaryText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryText
val SecondaryText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.secondaryText
val Border: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.border
val CardBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.card
val InputBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.input
val Accent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accent
val Success: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.success
val Error: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.error
val MutedText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mutedText
val DisabledText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledText
val SurfaceHover: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceHover
val SurfaceActive: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceActive
val SubtleBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.subtleBorder
val StrongBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.strongBorder
val AccentHover: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentHover
val Warning: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning
val Info: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.info
