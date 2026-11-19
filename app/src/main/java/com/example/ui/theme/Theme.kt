package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DeepEmeraldTeal,
    onPrimary = Color.White,
    primaryContainer = EmeraldTintContainer,
    onPrimaryContainer = DeepEmeraldTeal,
    secondary = SoftChampagneGold,
    onSecondary = DeepEmeraldTeal,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Color(0xFF5D4616),
    tertiary = SubtleDustyRose,
    onTertiary = Color.White,
    tertiaryContainer = DustyRoseContainerLight,
    onTertiaryContainer = Color(0xFF633636),
    background = WarmIvory,
    onBackground = DeepInkText,
    surface = WarmIvorySurface,
    onSurface = DeepInkText,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = MutedSageText,
    outline = SoftBorderLight,
    outlineVariant = Color(0xFFECE4D7),
    error = Color(0xFFB33A3A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftGoldDark,
    onPrimary = DeepMidnightTeal,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = WarmCreamText,
    secondary = EmeraldGlowDark,
    onSecondary = DeepMidnightTeal,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = SoftGoldDark,
    tertiary = DustyRoseDark,
    onTertiary = DeepMidnightTeal,
    tertiaryContainer = DustyRoseContainerDark,
    onTertiaryContainer = DustyRoseDark,
    background = DeepMidnightTeal,
    onBackground = WarmCreamText,
    surface = MidnightSurface,
    onSurface = WarmCreamText,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = MutedCreamText,
    outline = SoftBorderDark,
    outlineVariant = Color(0xFF1D3833),
    error = Color(0xFFE57373),
    onError = DeepMidnightTeal
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun TariqAlHalalTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
