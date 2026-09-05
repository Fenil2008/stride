package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StrydeColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = Color(0x2600E5C7),
    onPrimaryContainer = ElectricCyan,
    secondary = IndigoViolet,
    onSecondary = OffWhite,
    secondaryContainer = Color(0x266C5CE7),
    onSecondaryContainer = IndigoViolet,
    tertiary = MintGreen,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = OffWhite,
    surface = GraphiteCharcoal,
    onSurface = OffWhite,
    surfaceVariant = Color(0xFF222630),
    onSurfaceVariant = CoolGrey,
    outline = Color(0xFF2E3340),
    outlineVariant = Color(0xFF262A35),
    error = CoralRed,
    onError = ObsidianBlack
)

@Composable
fun StrydeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StrydeColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility aliases
@Composable
fun StrideTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    StrydeTheme(darkTheme = true, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    StrydeTheme(darkTheme = true, content = content)
}
