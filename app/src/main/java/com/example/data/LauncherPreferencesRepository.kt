package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DoubleTapAction
import com.example.model.LauncherGridSize
import com.example.model.LauncherPreferences
import com.example.model.PerformanceMode
import com.example.model.PixelIconPackMode
import com.example.model.PixelIconShape
import com.example.model.PixelNavBarStyle
import com.example.model.PixelWallpaperStyle
import com.example.ui.theme.PixelSurfaceMode
import com.example.ui.theme.PixelThemeAccent
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "pixel_theme_launcher_prefs")

class LauncherPreferencesRepository(private val context: Context) {

  private object Keys {
    val ACCENT = stringPreferencesKey("accent")
    val SURFACE_MODE = stringPreferencesKey("surface_mode")
    val WALLPAPER = stringPreferencesKey("wallpaper")
    val ICON_SHAPE = stringPreferencesKey("icon_shape")
    val ICON_PACK_MODE = stringPreferencesKey("icon_pack_mode")
    val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
    val GRID_SIZE = stringPreferencesKey("grid_size")
    val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
    val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
    val PIXEL_ACCENT_ICON_FRAME = booleanPreferencesKey("pixel_accent_icon_frame")
    val SHOW_HOME_LABELS = booleanPreferencesKey("show_home_labels")
    val SHOW_RAM_CHIP = booleanPreferencesKey("show_ram_chip")
    val PINNED_HOME_ORDER = stringPreferencesKey("pinned_home_order")
    val PINNED_DOCK_ORDER = stringPreferencesKey("pinned_dock_order")
    val HIDDEN_KEYS = stringSetPreferencesKey("hidden_keys")
    val RECENT_KEYS_ORDER = stringPreferencesKey("recent_keys_order")
    val INITIALIZED_DEFAULTS = booleanPreferencesKey("initialized_defaults")
  }

  val preferencesFlow: Flow<LauncherPreferences> =
    context.launcherDataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
        LauncherPreferences(
          accent = enumOrDefault(prefs[Keys.ACCENT], PixelThemeAccent.PIXEL_BLUE),
          surfaceMode = enumOrDefault(prefs[Keys.SURFACE_MODE], PixelSurfaceMode.PIXEL_DARK),
          wallpaper = enumOrDefault(prefs[Keys.WALLPAPER], PixelWallpaperStyle.PIXEL_HORIZON),
          iconShape = enumOrDefault(prefs[Keys.ICON_SHAPE], PixelIconShape.EIGHT_BIT_TILE),
          iconPackMode =
            enumOrDefault(prefs[Keys.ICON_PACK_MODE], PixelIconPackMode.CUSTOM_PIXEL_ART),
          navBarStyle = enumOrDefault(prefs[Keys.NAV_BAR_STYLE], PixelNavBarStyle.PIXEL_PILL),
          gridSize = enumOrDefault(prefs[Keys.GRID_SIZE], LauncherGridSize.BALANCED_4X5),
          performanceMode = enumOrDefault(prefs[Keys.PERFORMANCE_MODE], PerformanceMode.AUTO),
          doubleTapAction =
            enumOrDefault(prefs[Keys.DOUBLE_TAP_ACTION], DoubleTapAction.THEME_STUDIO),
          pixelAccentIconFrame = prefs[Keys.PIXEL_ACCENT_ICON_FRAME] ?: true,
          showHomeLabels = prefs[Keys.SHOW_HOME_LABELS] ?: true,
          showRamChip = prefs[Keys.SHOW_RAM_CHIP] ?: true,
          pinnedHomeKeys = decodeOrderedList(prefs[Keys.PINNED_HOME_ORDER]),
          pinnedDockKeys = decodeOrderedList(prefs[Keys.PINNED_DOCK_ORDER]),
          hiddenKeys = prefs[Keys.HIDDEN_KEYS] ?: emptySet(),
          recentKeys = decodeOrderedList(prefs[Keys.RECENT_KEYS_ORDER]),
          initializedDefaults = prefs[Keys.INITIALIZED_DEFAULTS] ?: false,
        )
      }

  suspend fun setAccent(accent: PixelThemeAccent) {
    context.launcherDataStore.edit { it[Keys.ACCENT] = accent.name }
  }

  suspend fun setSurfaceMode(mode: PixelSurfaceMode) {
    context.launcherDataStore.edit { it[Keys.SURFACE_MODE] = mode.name }
  }

  suspend fun setWallpaper(wallpaper: PixelWallpaperStyle) {
    context.launcherDataStore.edit { it[Keys.WALLPAPER] = wallpaper.name }
  }

  suspend fun setIconShape(shape: PixelIconShape) {
    context.launcherDataStore.edit { it[Keys.ICON_SHAPE] = shape.name }
  }

  suspend fun setIconPackMode(mode: PixelIconPackMode) {
    context.launcherDataStore.edit { it[Keys.ICON_PACK_MODE] = mode.name }
  }

  suspend fun setNavBarStyle(style: PixelNavBarStyle) {
    context.launcherDataStore.edit { it[Keys.NAV_BAR_STYLE] = style.name }
  }

  suspend fun setGridSize(gridSize: LauncherGridSize) {
    context.launcherDataStore.edit { it[Keys.GRID_SIZE] = gridSize.name }
  }

  suspend fun setPerformanceMode(mode: PerformanceMode) {
    context.launcherDataStore.edit { it[Keys.PERFORMANCE_MODE] = mode.name }
  }

  suspend fun setDoubleTapAction(action: DoubleTapAction) {
    context.launcherDataStore.edit { it[Keys.DOUBLE_TAP_ACTION] = action.name }
  }

  suspend fun setPixelAccentIconFrame(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.PIXEL_ACCENT_ICON_FRAME] = enabled }
  }

  suspend fun setShowHomeLabels(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.SHOW_HOME_LABELS] = enabled }
  }

  suspend fun setShowRamChip(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.SHOW_RAM_CHIP] = enabled }
  }

  suspend fun saveInitialDefaults(homeKeys: List<String>, dockKeys: List<String>) {
    context.launcherDataStore.edit { prefs ->
      if (prefs[Keys.INITIALIZED_DEFAULTS] != true) {
        prefs[Keys.PINNED_HOME_ORDER] = encodeOrderedList(homeKeys)
        prefs[Keys.PINNED_DOCK_ORDER] = encodeOrderedList(dockKeys)
        prefs[Keys.INITIALIZED_DEFAULTS] = true
      }
    }
  }

  suspend fun togglePinHome(componentKey: String, maxHomeItems: Int) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeOrderedList(prefs[Keys.PINNED_HOME_ORDER]).toMutableList()
      if (current.contains(componentKey)) {
        current.remove(componentKey)
      } else {
        if (current.size >= maxHomeItems) {
          current.removeAt(0)
        }
        current.add(componentKey)
      }
      prefs[Keys.PINNED_HOME_ORDER] = encodeOrderedList(current)
    }
  }

  suspend fun togglePinDock(componentKey: String, maxDockItems: Int = 5) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeOrderedList(prefs[Keys.PINNED_DOCK_ORDER]).toMutableList()
      if (current.contains(componentKey)) {
        current.remove(componentKey)
      } else {
        if (current.size >= maxDockItems) {
          current.removeAt(current.lastIndex)
        }
        current.add(componentKey)
      }
      prefs[Keys.PINNED_DOCK_ORDER] = encodeOrderedList(current)
    }
  }

  suspend fun toggleHidden(componentKey: String) {
    context.launcherDataStore.edit { prefs ->
      val currentHidden = (prefs[Keys.HIDDEN_KEYS] ?: emptySet()).toMutableSet()
      if (currentHidden.contains(componentKey)) {
        currentHidden.remove(componentKey)
      } else {
        currentHidden.add(componentKey)
        val home = decodeOrderedList(prefs[Keys.PINNED_HOME_ORDER]).filterNot { it == componentKey }
        val dock = decodeOrderedList(prefs[Keys.PINNED_DOCK_ORDER]).filterNot { it == componentKey }
        prefs[Keys.PINNED_HOME_ORDER] = encodeOrderedList(home)
        prefs[Keys.PINNED_DOCK_ORDER] = encodeOrderedList(dock)
      }
      prefs[Keys.HIDDEN_KEYS] = currentHidden
    }
  }

  suspend fun recordRecentLaunch(componentKey: String) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeOrderedList(prefs[Keys.RECENT_KEYS_ORDER]).toMutableList()
      current.remove(componentKey)
      current.add(0, componentKey)
      prefs[Keys.RECENT_KEYS_ORDER] = encodeOrderedList(current.take(8))
    }
  }

  private fun encodeOrderedList(items: List<String>): String = items.joinToString(separator = "|")

  private fun decodeOrderedList(raw: String?): List<String> =
    if (raw.isNullOrBlank()) emptyList() else raw.split("|").filter { it.isNotBlank() }

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw == null) return default
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
  }
}
