package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// STRYDE PRODUCTION COLOR PALETTE
// ============================================================================

// 1. Primary Background: Deep obsidian black
val ObsidianBlack = Color(0xFF0B0D12)
val BackgroundDark = ObsidianBlack
val BackgroundLight = ObsidianBlack

// 2. Surface / Card Background: Graphite charcoal
val GraphiteCharcoal = Color(0xFF1A1D24)
val GraphiteSurface = GraphiteCharcoal
val SurfaceDark = GraphiteCharcoal
val SurfaceLight = GraphiteCharcoal

// 3. Primary Accent: Electric cyan-teal (replaces orange completely)
val ElectricCyan = Color(0xFF00E5C7)
val ElectricCyanDark = Color(0xFF00BFA5)
val ElectricCyanLight = Color(0xFF33EBCE)
val ElectricCyanSubtle = Color(0x2600E5C7)

// 4. Secondary Accent: Indigo-violet (secondary highlights, buttons, gradients)
val IndigoViolet = Color(0xFF6C5CE7)
val IndigoVioletDark = Color(0xFF5849BE)
val IndigoVioletLight = Color(0xFF8A7CF0)
val IndigoVioletSubtle = Color(0x266C5CE7)

// 5. Text Primary: Off-white (soft, high-contrast, premium)
val OffWhite = Color(0xFFEAEAF0)
val TextPrimary = OffWhite

// 6. Text Muted / Secondary: Cool grey
val CoolGrey = Color(0xFF8A8F9C)
val TextSecondary = CoolGrey
val TextMuted = CoolGrey

// 7. Success / Positive (kudos, completed states): Mint green
val MintGreen = Color(0xFF3DDC97)
val SuccessGreen = MintGreen

// 8. Error / Warning: Soft coral red
val CoralRed = Color(0xFFFF6B6B)
val SoftCoral = CoralRed
val ErrorRed = CoralRed

// 9. Map Route Polylines:
val RoutePrimary = ElectricCyan       // #00E5C7 primary live route
val RouteSecondary = IndigoViolet     // #6C5CE7 secondary / past routes

// ============================================================================
// COMPATIBILITY ALIASES (Guarantees zero legacy orange / plain white leaks)
// ============================================================================
val StravaOrange = ElectricCyan
val StravaOrangeDark = ElectricCyanDark
val StravaOrangeLight = ElectricCyanLight
val StravaOrangeSubtle = ElectricCyanSubtle
val OrangeGlow = ElectricCyanSubtle

val AccentBlue = IndigoViolet
val AccentGreen = MintGreen
val AccentYellow = Color(0xFFF59E0B)

val Slate900 = OffWhite           // Crisp headers on dark
val Slate800 = Color(0xFFD4D6E0)
val Slate700 = Color(0xFFB5BAC9)
val Slate600 = CoolGrey          // #8A8F9C
val Slate500 = CoolGrey
val Slate400 = CoolGrey
val Slate300 = Color(0xFF2C303B)
val Slate200 = Color(0xFF262933)
val Slate100 = Color(0xFF1F222B)
val Slate50 = GraphiteCharcoal   // #1A1D24

// Liquid Glass Styling Tokens
val FrostedGlassWhite = Color(0xCC1A1D24)
val FrostedGlassLight = Color(0xE61A1D24)
val FrostedGlassBorder = Color(0x3300E5C7)
val FrostedGlassBorderSubtle = Color(0x268A8F9C)
val FrostedGlassShadow = Color(0x80000000)

val GlassBackgroundLight = Color(0xD91A1D24)
val GlassBackgroundDark = Color(0xD91A1D24)
val GlassBorderLight = Color(0x3300E5C7)
val GlassBorderDark = Color(0x3300E5C7)
