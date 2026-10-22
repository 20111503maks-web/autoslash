package com.autoslash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autoslash.domain.Platform

object AppColors {
    val Background = Color(0xFF0B0B12)
    val Surface = Color(0xFF16161F)
    val SurfaceHigh = Color(0xFF1F1F2B)
    val Outline = Color(0xFF2C2C3A)
    val TextPrimary = Color(0xFFF4F4F8)
    val TextSecondary = Color(0xFF9E9EB2)

    val TikTokCyan = Color(0xFF25F4EE)
    val TikTokPink = Color(0xFFFE2C55)
    val YouTubeRed = Color(0xFFFF0033)
    val YouTubeOrange = Color(0xFFFF6B4A)

    val Success = Color(0xFF2EE59D)
    val Warning = Color(0xFFFFC542)
    val Paused = Color(0xFFFF8A3D)
    val Danger = Color(0xFFE53950)
}

/** Accent colors of each platform, used to highlight the selected card and primary buttons. */
data class PlatformAccent(val primary: Color, val secondary: Color) {
    val brush: Brush get() = Brush.linearGradient(listOf(primary, secondary))
}

val Platform.accent: PlatformAccent
    get() = when (this) {
        Platform.TIKTOK -> PlatformAccent(AppColors.TikTokCyan, AppColors.TikTokPink)
        Platform.YOUTUBE_SHORTS -> PlatformAccent(AppColors.YouTubeRed, AppColors.YouTubeOrange)
    }

private val colorScheme = darkColorScheme(
    primary = AppColors.TikTokPink,
    onPrimary = Color.White,
    secondary = AppColors.TikTokCyan,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Outline,
    error = AppColors.Danger,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun AutoSlashTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
