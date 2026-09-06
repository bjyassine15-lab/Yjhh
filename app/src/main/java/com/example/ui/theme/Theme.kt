package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RafiqahRose,
    onPrimary = Color.White,
    primaryContainer = RafiqahRoseContainer,
    onPrimaryContainer = RafiqahOnRoseContainer,
    secondary = SageOlive,
    onSecondary = Color.White,
    secondaryContainer = SageOliveContainer,
    onSecondaryContainer = OnSageOliveContainer,
    tertiary = TerracottaAccent,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = Color(0xFF4A1A0B),
    background = WarmCreamBackground,
    onBackground = DeepCharcoalText,
    surface = WarmSurface,
    onSurface = DeepCharcoalText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MediumMutedText,
    outline = SubtleBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5B3C2),
    onPrimary = Color(0xFF5A1125),
    primaryContainer = Color(0xFF7A253A),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFA8D3B4),
    onSecondary = Color(0xFF143820),
    secondaryContainer = Color(0xFF2C5138),
    onSecondaryContainer = Color(0xFFC4EFCF),
    tertiary = Color(0xFFFFB59D),
    onTertiary = Color(0xFF5E1B05),
    background = Color(0xFF181517),
    onBackground = Color(0xFFEFE8E9),
    surface = Color(0xFF221E20),
    onSurface = Color(0xFFEFE8E9),
    surfaceVariant = Color(0xFF332B2E),
    onSurfaceVariant = Color(0xFFD2C4C7)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cohesive warm identity by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
