package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated accent palettes inspired by gcantoni/PixelTheme (Pixel Blue, Ocean, Cinnamon,
 * Orchid, Space, and Emerald) adapted for crisp pixel-art contrast on both dark & light surfaces.
 */
enum class PixelThemeAccent(
  val displayName: String,
  val primaryDark: Color,
  val onPrimaryDark: Color,
  val primaryContainerDark: Color,
  val onPrimaryContainerDark: Color,
  val primaryLight: Color,
  val onPrimaryLight: Color,
  val primaryContainerLight: Color,
  val onPrimaryContainerLight: Color,
  val secondaryAccent: Color,
) {
  PIXEL_BLUE(
    displayName = "Pixel Blue",
    primaryDark = Color(0xFF8AB4F8),
    onPrimaryDark = Color(0xFF062E6F),
    primaryContainerDark = Color(0xFF1A438A),
    onPrimaryContainerDark = Color(0xFFD2E3FC),
    primaryLight = Color(0xFF1A73E8),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFD2E3FC),
    onPrimaryContainerLight = Color(0xFF041E49),
    secondaryAccent = Color(0xFF34A853),
  ),
  OCEAN_TEAL(
    displayName = "Ocean",
    primaryDark = Color(0xFF4DB6AC),
    onPrimaryDark = Color(0xFF003733),
    primaryContainerDark = Color(0xFF00504B),
    onPrimaryContainerDark = Color(0xFFB2DFDB),
    primaryLight = Color(0xFF00796B),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFB2DFDB),
    onPrimaryContainerLight = Color(0xFF00201D),
    secondaryAccent = Color(0xFF26C6DA),
  ),
  CINNAMON(
    displayName = "Cinnamon",
    primaryDark = Color(0xFFE5988B),
    onPrimaryDark = Color(0xFF4A1912),
    primaryContainerDark = Color(0xFF6E2E24),
    onPrimaryContainerDark = Color(0xFFFFDAD4),
    primaryLight = Color(0xFFAF6050),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFFDAD4),
    onPrimaryContainerLight = Color(0xFF3B0905),
    secondaryAccent = Color(0xFFFBBC04),
  ),
  ORCHID(
    displayName = "Orchid",
    primaryDark = Color(0xFFDF94E8),
    onPrimaryDark = Color(0xFF481054),
    primaryContainerDark = Color(0xFF662973),
    onPrimaryContainerDark = Color(0xFFFCD7FF),
    primaryLight = Color(0xFF9C27B0),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFCD7FF),
    onPrimaryContainerLight = Color(0xFF33003F),
    secondaryAccent = Color(0xFF7C4DFF),
  ),
  SPACE_INDIGO(
    displayName = "Space",
    primaryDark = Color(0xFF9FA8DA),
    onPrimaryDark = Color(0xFF141B4D),
    primaryContainerDark = Color(0xFF2C387E),
    onPrimaryContainerDark = Color(0xFFE8EAF6),
    primaryLight = Color(0xFF3F51B5),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFC5CAE9),
    onPrimaryContainerLight = Color(0xFF0A1045),
    secondaryAccent = Color(0xFF536DFE),
  ),
  PIXEL_EMERALD(
    displayName = "Emerald",
    primaryDark = Color(0xFF81C784),
    onPrimaryDark = Color(0xFF093911),
    primaryContainerDark = Color(0xFF1B5E20),
    onPrimaryContainerDark = Color(0xFFC8E6C9),
    primaryLight = Color(0xFF2E7D32),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFC8E6C9),
    onPrimaryContainerLight = Color(0xFF002105),
    secondaryAccent = Color(0xFF00BFA5),
  ),
}

enum class PixelSurfaceMode(val displayName: String, val isDark: Boolean) {
  PIXEL_DARK("Pixel Dark", true),
  AMOLED_BLACK("AMOLED Black", true),
  CRISP_LIGHT("Crisp Light", false),
}

// Pixel Dark base tokens
val PixelDarkBackground = Color(0xFF111318)
val PixelDarkSurface = Color(0xFF191C24)
val PixelDarkSurfaceVariant = Color(0xFF252A36)
val PixelDarkOnSurface = Color(0xFFE6E8EF)
val PixelDarkOnSurfaceVariant = Color(0xFFB3B9C9)
val PixelDarkOutline = Color(0xFF42495C)

// AMOLED Pitch Black base tokens (battery + low-end OLED friendly)
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0B0D12)
val AmoledSurfaceVariant = Color(0xFF161922)
val AmoledOnSurface = Color(0xFFF0F2F8)
val AmoledOnSurfaceVariant = Color(0xFFA7AEC0)
val AmoledOutline = Color(0xFF313748)

// Crisp Pixel Light base tokens
val PixelLightBackground = Color(0xFFF3F5FA)
val PixelLightSurface = Color(0xFFFFFFFF)
val PixelLightSurfaceVariant = Color(0xFFE4E8F2)
val PixelLightOnSurface = Color(0xFF171A23)
val PixelLightOnSurfaceVariant = Color(0xFF525A6E)
val PixelLightOutline = Color(0xFFB5BDD1)
