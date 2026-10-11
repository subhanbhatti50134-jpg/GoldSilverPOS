package com.goldlab.management.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// SNJ Gold Lab — Luxury Black & Gold palette
private val RoyalGold = Color(0xFFE0BD59)
private val BrightGold = Color(0xFFFFD978)
private val DeepGold = Color(0xFF8F6B19)
private val Platinum = Color(0xFFD8DCE4)

private val PureBlack = Color(0xFF08090B)
private val DarkBackground = Color(0xFF0D0E11)
private val CardSurface = Color(0xFF17191E)
private val RaisedSurface = Color(0xFF202229)
private val SoftBorder = Color(0xFF35312A)

private val LuxuryColors = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color(0xFF171106),

    primaryContainer = DeepGold,
    onPrimaryContainer = Color(0xFFFFF1C4),

    secondary = Platinum,
    onSecondary = Color(0xFF15171B),

    secondaryContainer = Color(0xFF30343C),
    onSecondaryContainer = Color(0xFFE8EAF0),

    tertiary = BrightGold,
    onTertiary = Color(0xFF201804),

    background = DarkBackground,
    onBackground = Color(0xFFF5F1E7),

    surface = CardSurface,
    onSurface = Color(0xFFF5F1E7),

    surfaceVariant = RaisedSurface,
    onSurfaceVariant = Color(0xFFC8C4B9),

    outline = Color(0xFF77643A),
    outlineVariant = SoftBorder,

    error = Color(0xFFFF6B70),
    onError = Color(0xFF300609),

    scrim = Color(0xCC000000),

    inverseSurface = Color(0xFFF5F1E7),
    inverseOnSurface = Color(0xFF171719),
    inversePrimary = DeepGold
)

private val LuxuryTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 25.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun LabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuxuryColors,
        typography = LuxuryTypography,
        content = content
    )
}
