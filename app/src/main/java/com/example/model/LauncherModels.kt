package com.example.model

import com.example.ui.theme.PixelSurfaceMode
import com.example.ui.theme.PixelThemeAccent

enum class PixelWallpaperStyle(val displayName: String, val isDarkFriendly: Boolean) {
  PIXEL_HORIZON("Pixel Horizon", true),
  RETRO_NIGHT("Retro Night", true),
  PASTEL_SKY("Pastel Sky", false),
  ZERO_RAM_GRID("Matrix Grid (0MB)", true),
}

enum class PixelIconShape(val displayName: String) {
  EIGHT_BIT_TILE("8-Bit Tile"),
  PIXEL_SQUIRCLE("Pixel Squircle"),
  RETRO_BEVEL("Retro Bevel"),
  CLEAN_CIRCLE("Clean Circle"),
}

enum class PixelIconPackMode(val displayName: String, val subtitle: String) {
  CUSTOM_PIXEL_ART("Pixel-Art Icons", "Zero-RAM 12x12 pixel sprites & 8-bit badges"),
  PIXELIZED_SYSTEM("8-Bit Downsampled", "20x20 nearest-neighbor pixelized app icons (~1.6 KB)"),
  CLASSIC_FRAMED("Classic Framed", "Standard cached app icons inside pixel frames"),
}

/** Homage to gcantoni/PixelTheme's iconic navigation bar customization options. */
enum class PixelNavBarStyle(val displayName: String) {
  PIXEL_PILL("Pixel Pill"),
  AOSP_RETRO("AOSP Retro"),
  FILLED_BAR("Filled Bar"),
}

enum class LauncherGridSize(val displayName: String, val columns: Int, val homeRows: Int) {
  COMPACT_4X4("4 x 4", 4, 4),
  BALANCED_4X5("4 x 5", 4, 5),
  DENSE_5X5("5 x 5", 5, 5),
}

enum class PerformanceMode(val displayName: String, val description: String) {
  AUTO("Auto (Smart)", "Enables Lite Mode automatically on <= 2.5 GB RAM devices"),
  LITE_ALWAYS("Lite Always", "Reduces bitmap memory, overdraw & spring physics for older CPUs"),
  FULL_MOTION("Full Motion", "Smooth spring physics and full-resolution pixel backdrops"),
}

enum class DoubleTapAction(val displayName: String) {
  THEME_STUDIO("Open Theme Studio"),
  QUICK_SEARCH("Open Quick Search"),
  APP_DRAWER("Open App Drawer"),
  NONE("Disabled"),
}

data class InstalledApp(
  val packageName: String,
  val activityName: String,
  val label: String,
  val isSystemApp: Boolean,
) {
  val componentKey: String
    get() = "$packageName/$activityName"
}

data class LauncherPreferences(
  val accent: PixelThemeAccent = PixelThemeAccent.PIXEL_BLUE,
  val surfaceMode: PixelSurfaceMode = PixelSurfaceMode.PIXEL_DARK,
  val wallpaper: PixelWallpaperStyle = PixelWallpaperStyle.PIXEL_HORIZON,
  val iconShape: PixelIconShape = PixelIconShape.EIGHT_BIT_TILE,
  val iconPackMode: PixelIconPackMode = PixelIconPackMode.CUSTOM_PIXEL_ART,
  val navBarStyle: PixelNavBarStyle = PixelNavBarStyle.PIXEL_PILL,
  val gridSize: LauncherGridSize = LauncherGridSize.BALANCED_4X5,
  val performanceMode: PerformanceMode = PerformanceMode.AUTO,
  val doubleTapAction: DoubleTapAction = DoubleTapAction.THEME_STUDIO,
  val pixelAccentIconFrame: Boolean = true,
  val showHomeLabels: Boolean = true,
  val showRamChip: Boolean = true,
  val pinnedHomeKeys: List<String> = emptyList(),
  val pinnedDockKeys: List<String> = emptyList(),
  val hiddenKeys: Set<String> = emptySet(),
  val recentKeys: List<String> = emptyList(),
  val initializedDefaults: Boolean = false,
)

data class DeviceTelemetry(
  val totalRamMb: Long = 0L,
  val availRamMb: Long = 0L,
  val isLowRamDevice: Boolean = false,
  val batteryPercent: Int = 100,
  val isCharging: Boolean = false,
  val freeStorageGb: Float = 0f,
  val cachedIconCount: Int = 0,
  val effectiveLiteMode: Boolean = false,
)

sealed interface LauncherOverlay {
  data object None : LauncherOverlay

  data object AppDrawer : LauncherOverlay

  data object QuickSearch : LauncherOverlay

  data object ThemeStudio : LauncherOverlay
}
