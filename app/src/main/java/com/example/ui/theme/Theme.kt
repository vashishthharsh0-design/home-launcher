package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PixelExtendedColors(
  val accent: PixelThemeAccent,
  val surfaceMode: PixelSurfaceMode,
  val pixelFrameBorder: Color,
  val pixelTileFill: Color,
  val pixelBadgeSecondary: Color,
  val scrimSurface: Color,
)

val LocalPixelExtendedColors = staticCompositionLocalOf {
  PixelExtendedColors(
    accent = PixelThemeAccent.PIXEL_BLUE,
    surfaceMode = PixelSurfaceMode.PIXEL_DARK,
    pixelFrameBorder = PixelDarkOutline,
    pixelTileFill = PixelDarkSurface,
    pixelBadgeSecondary = PixelThemeAccent.PIXEL_BLUE.secondaryAccent,
    scrimSurface = Color(0xCC111318),
  )
}

fun buildPixelColorScheme(
  accent: PixelThemeAccent,
  surfaceMode: PixelSurfaceMode,
): ColorScheme {
  return when (surfaceMode) {
    PixelSurfaceMode.PIXEL_DARK ->
      darkColorScheme(
        primary = accent.primaryDark,
        onPrimary = accent.onPrimaryDark,
        primaryContainer = accent.primaryContainerDark,
        onPrimaryContainer = accent.onPrimaryContainerDark,
        secondary = accent.secondaryAccent,
        onSecondary = Color(0xFF071B0E),
        secondaryContainer = Color(0xFF202531),
        onSecondaryContainer = PixelDarkOnSurface,
        background = PixelDarkBackground,
        onBackground = PixelDarkOnSurface,
        surface = PixelDarkSurface,
        onSurface = PixelDarkOnSurface,
        surfaceVariant = PixelDarkSurfaceVariant,
        onSurfaceVariant = PixelDarkOnSurfaceVariant,
        outline = PixelDarkOutline,
      )
    PixelSurfaceMode.AMOLED_BLACK ->
      darkColorScheme(
        primary = accent.primaryDark,
        onPrimary = accent.onPrimaryDark,
        primaryContainer = accent.primaryContainerDark,
        onPrimaryContainer = accent.onPrimaryContainerDark,
        secondary = accent.secondaryAccent,
        onSecondary = Color(0xFF071B0E),
        secondaryContainer = AmoledSurfaceVariant,
        onSecondaryContainer = AmoledOnSurface,
        background = AmoledBackground,
        onBackground = AmoledOnSurface,
        surface = AmoledSurface,
        onSurface = AmoledOnSurface,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = AmoledOnSurfaceVariant,
        outline = AmoledOutline,
      )
    PixelSurfaceMode.CRISP_LIGHT ->
      lightColorScheme(
        primary = accent.primaryLight,
        onPrimary = accent.onPrimaryLight,
        primaryContainer = accent.primaryContainerLight,
        onPrimaryContainer = accent.onPrimaryContainerLight,
        secondary = accent.secondaryAccent,
        onSecondary = Color.White,
        secondaryContainer = PixelLightSurfaceVariant,
        onSecondaryContainer = PixelLightOnSurface,
        background = PixelLightBackground,
        onBackground = PixelLightOnSurface,
        surface = PixelLightSurface,
        onSurface = PixelLightOnSurface,
        surfaceVariant = PixelLightSurfaceVariant,
        onSurfaceVariant = PixelLightOnSurfaceVariant,
        outline = PixelLightOutline,
      )
  }
}

@Composable
fun PixelThemeWrapper(
  accent: PixelThemeAccent = PixelThemeAccent.PIXEL_BLUE,
  surfaceMode: PixelSurfaceMode = PixelSurfaceMode.PIXEL_DARK,
  content: @Composable () -> Unit,
) {
  val colorScheme = buildPixelColorScheme(accent, surfaceMode)
  val extended =
    when (surfaceMode) {
      PixelSurfaceMode.PIXEL_DARK ->
        PixelExtendedColors(
          accent = accent,
          surfaceMode = surfaceMode,
          pixelFrameBorder = PixelDarkOutline,
          pixelTileFill = PixelDarkSurface,
          pixelBadgeSecondary = accent.secondaryAccent,
          scrimSurface = Color(0xD9111318),
        )
      PixelSurfaceMode.AMOLED_BLACK ->
        PixelExtendedColors(
          accent = accent,
          surfaceMode = surfaceMode,
          pixelFrameBorder = AmoledOutline,
          pixelTileFill = AmoledSurface,
          pixelBadgeSecondary = accent.secondaryAccent,
          scrimSurface = Color(0xEB000000),
        )
      PixelSurfaceMode.CRISP_LIGHT ->
        PixelExtendedColors(
          accent = accent,
          surfaceMode = surfaceMode,
          pixelFrameBorder = PixelLightOutline,
          pixelTileFill = PixelLightSurface,
          pixelBadgeSecondary = accent.secondaryAccent,
          scrimSurface = Color(0xE6F3F5FA),
        )
    }

  CompositionLocalProvider(LocalPixelExtendedColors provides extended) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = PixelThemeTypography,
      content = content,
    )
  }
}
