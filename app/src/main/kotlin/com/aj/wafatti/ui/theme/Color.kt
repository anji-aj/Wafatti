package com.aj.wafatti.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Wafatti Brand Colors (Royal Purple & Vibrant Pink/Magenta)
val WafattiPurplePrimary = Color(0xFF7C3AED)      // Rich Royal Purple
val WafattiPurpleDark = Color(0xFF5B21B6)         // Deep Royal Purple
val WafattiPurpleLight = Color(0xFFA78BFA)        // Soft Purple Highlight
val WafattiPinkSecondary = Color(0xFFEC4899)      // Vibrant Pink / Magenta
val WafattiPinkDark = Color(0xFFBE185D)           // Deep Magenta Accent
val WafattiPinkLight = Color(0xFFF472B6)          // Bright Pink Accent

// Dark Theme Surfaces & Backgrounds
val WafattiDarkBackground = Color(0xFF0D0817)     // Near-black with deep purple undertone
val WafattiDarkSurface = Color(0xFF160F26)        // Very dark purple-tinted surface
val WafattiDarkSurfaceVariant = Color(0xFF231838) // Dark charcoal/purple card/input surface
val WafattiDarkContainer = Color(0xFF311F4E)      // Container surface
val WafattiDarkOutline = Color(0xFF4A3268)        // Borders and dividers

// Light Theme Surfaces & Backgrounds
val WafattiLightBackground = Color(0xFFF8F5FF)    // Very light lavender/neutral background
val WafattiLightSurface = Color(0xFFFFFFFF)       // Pure white surface
val WafattiLightSurfaceVariant = Color(0xFFF1E9FE) // Soft lavender-tinted surface
val WafattiLightContainer = Color(0xFFEDE4FF)     // Light purple container
val WafattiLightOutline = Color(0xFFD8B4FE)       // Soft purple outline

// Text Colors
val WafattiTextPrimaryDark = Color(0xFFF5F3FF)    // High contrast white/lavender for dark theme
val WafattiTextSecondaryDark = Color(0xFFC4B5FD)  // Soft lavender for dark theme
val WafattiTextPrimaryLight = Color(0xFF1E1035)   // Dark purple/black for light theme
val WafattiTextSecondaryLight = Color(0xFF5B4A78) // Muted purple-gray for light theme

// Semantic Colors
val WafattiSuccess = Color(0xFF22C55E)
val WafattiWarning = Color(0xFFF59E0B)
val WafattiError = Color(0xFFEF4444)

// Brand Gradient (Purple -> Pink/Magenta Accent)
val WafattiGradientBrush = Brush.horizontalGradient(
    colors = listOf(WafattiPurplePrimary, WafattiPinkSecondary)
)
val WafattiGradientVertical = Brush.verticalGradient(
    colors = listOf(WafattiPurplePrimary, WafattiPinkSecondary)
)
