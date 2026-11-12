package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EnigmaDarkColorScheme = darkColorScheme(
    primary = EnigmaPrimary,
    onPrimary = EnigmaOnPrimary,
    primaryContainer = EnigmaPrimaryContainer,
    onPrimaryContainer = EnigmaOnPrimaryContainer,
    secondary = EnigmaGold,
    onSecondary = EnigmaOnGold,
    secondaryContainer = EnigmaGoldContainer,
    onSecondaryContainer = EnigmaOnGoldContainer,
    tertiary = EnigmaCyan,
    onTertiary = EnigmaOnCyan,
    background = MysteryDarkBackground,
    onBackground = EnigmaTextPrimary,
    surface = MysteryDarkSurface,
    onSurface = EnigmaTextPrimary,
    surfaceVariant = MysteryDarkSurfaceVariant,
    onSurfaceVariant = EnigmaTextSecondary,
    outline = EnigmaBorder,
    outlineVariant = MysteryDarkSurfaceVariant
)

@Composable
fun EnigmaAtlasTheme(
    content: @Composable () -> Unit
) {
    // La app está expresamente diseñada con una estética nocturna y atmosférica
    MaterialTheme(
        colorScheme = EnigmaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
