package com.example.ui

import android.app.Application
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InstalledAppsRepository
import com.example.data.LauncherPreferencesRepository
import com.example.model.DeviceTelemetry
import com.example.model.DoubleTapAction
import com.example.model.InstalledApp
import com.example.model.LauncherGridSize
import com.example.model.LauncherOverlay
import com.example.model.LauncherPreferences
import com.example.model.PerformanceMode
import com.example.model.PixelIconPackMode
import com.example.model.PixelIconShape
import com.example.model.PixelNavBarStyle
import com.example.model.PixelWallpaperStyle
import com.example.ui.theme.PixelSurfaceMode
import com.example.ui.theme.PixelThemeAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherUiState(
  val isLoading: Boolean = true,
  val preferences: LauncherPreferences = LauncherPreferences(),
  val allApps: List<InstalledApp> = emptyList(),
  val visibleDrawerApps: List<InstalledApp> = emptyList(),
  val homeApps: List<InstalledApp> = emptyList(),
  val dockApps: List<InstalledApp> = emptyList(),
  val recentApps: List<InstalledApp> = emptyList(),
  val hiddenApps: List<InstalledApp> = emptyList(),
  val telemetry: DeviceTelemetry = DeviceTelemetry(),
  val isDefaultLauncher: Boolean = false,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val prefsRepository = LauncherPreferencesRepository(application)
  private val appsRepository = InstalledAppsRepository(application)

  private val _allApps = MutableStateFlow<List<InstalledApp>>(emptyList())
  private val _isLoading = MutableStateFlow(true)
  private val _telemetry = MutableStateFlow(DeviceTelemetry())
  private val _isDefaultLauncher = MutableStateFlow(false)
  private val _overlay = MutableStateFlow<LauncherOverlay>(LauncherOverlay.None)
  private val _searchQuery = MutableStateFlow("")
  private val _selectedAppForAction = MutableStateFlow<InstalledApp?>(null)

  val overlay: StateFlow<LauncherOverlay> = _overlay.asStateFlow()
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
  val selectedAppForAction: StateFlow<InstalledApp?> = _selectedAppForAction.asStateFlow()

  val uiState: StateFlow<LauncherUiState> =
    combine(
        prefsRepository.preferencesFlow,
        _allApps,
        _isLoading,
        _telemetry,
        _isDefaultLauncher,
      ) { prefs, apps, loading, telemetry, isDefault ->
        val byKey = apps.associateBy { it.componentKey }
        val homeApps = prefs.pinnedHomeKeys.mapNotNull { byKey[it] }
        val dockApps = prefs.pinnedDockKeys.mapNotNull { byKey[it] }
        val recentApps = prefs.recentKeys.mapNotNull { byKey[it] }.filterNot {
          it.componentKey in prefs.hiddenKeys
        }
        val visibleDrawer = apps.filterNot { it.componentKey in prefs.hiddenKeys }
        val hiddenApps = apps.filter { it.componentKey in prefs.hiddenKeys }

        LauncherUiState(
          isLoading = loading,
          preferences = prefs,
          allApps = apps,
          visibleDrawerApps = visibleDrawer,
          homeApps = homeApps,
          dockApps = dockApps,
          recentApps = recentApps,
          hiddenApps = hiddenApps,
          telemetry = telemetry,
          isDefaultLauncher = isDefault,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
      )

  init {
    refreshAll()
  }

  fun refreshAll() {
    viewModelScope.launch {
      _isLoading.value = true
      val apps = appsRepository.loadInstalledApps()
      _allApps.value = apps
      _isDefaultLauncher.value = appsRepository.isDefaultLauncher()

      val currentPrefs = uiState.value.preferences
      if (!currentPrefs.initializedDefaults && apps.isNotEmpty()) {
        seedInitialHomeAndDock(apps)
      }

      refreshTelemetry(currentPrefs.performanceMode)
      _isLoading.value = false
    }
  }

  fun refreshTelemetryOnly() {
    val mode = uiState.value.preferences.performanceMode
    refreshTelemetry(mode)
    _isDefaultLauncher.value = appsRepository.isDefaultLauncher()
  }

  private fun refreshTelemetry(mode: PerformanceMode) {
    _telemetry.value = appsRepository.readDeviceTelemetry(mode)
  }

  private suspend fun seedInitialHomeAndDock(apps: List<InstalledApp>) {
    val preferredDockKeywords = listOf("dialer", "phone", "messaging", "mms", "browser", "chrome", "camera")
    val preferredHomeKeywords =
      listOf("settings", "gallery", "photos", "clock", "calendar", "play", "vending", "music", "calculator", "files", "contacts")

    val dockApps = mutableListOf<InstalledApp>()
    for (keyword in preferredDockKeywords) {
      if (dockApps.size >= 4) break
      val match =
        apps.firstOrNull { app ->
          app !in dockApps &&
            (app.packageName.lowercase().contains(keyword) ||
              app.label.lowercase().contains(keyword))
        }
      if (match != null) dockApps.add(match)
    }
    for (app in apps) {
      if (dockApps.size >= 4) break
      if (app !in dockApps) dockApps.add(app)
    }

    val homeApps = mutableListOf<InstalledApp>()
    for (keyword in preferredHomeKeywords) {
      if (homeApps.size >= 8) break
      val match =
        apps.firstOrNull { app ->
          app !in dockApps &&
            app !in homeApps &&
            (app.packageName.lowercase().contains(keyword) ||
              app.label.lowercase().contains(keyword))
        }
      if (match != null) homeApps.add(match)
    }
    for (app in apps) {
      if (homeApps.size >= 8) break
      if (app !in dockApps && app !in homeApps) homeApps.add(app)
    }

    prefsRepository.saveInitialDefaults(
      homeKeys = homeApps.map { it.componentKey },
      dockKeys = dockApps.map { it.componentKey },
    )
  }

  suspend fun loadAppIcon(
    app: InstalledApp,
    liteMode: Boolean,
    pixelize: Boolean = false,
  ): ImageBitmap? {
    return appsRepository.getOrLoadIcon(app, liteMode, pixelize)
  }

  fun getCachedAppIcon(
    app: InstalledApp,
    liteMode: Boolean,
    pixelize: Boolean = false,
  ): ImageBitmap? {
    return appsRepository.getCachedIconImmediate(app, liteMode, pixelize)
  }

  fun launchApp(app: InstalledApp) {
    val launched = appsRepository.launchApp(app)
    if (launched) {
      _overlay.value = LauncherOverlay.None
      _searchQuery.value = ""
      viewModelScope.launch { prefsRepository.recordRecentLaunch(app.componentKey) }
    }
  }

  fun openOverlay(target: LauncherOverlay) {
    if (target == LauncherOverlay.None) {
      _searchQuery.value = ""
    }
    _overlay.value = target
    refreshTelemetryOnly()
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openAppActions(app: InstalledApp) {
    _selectedAppForAction.value = app
  }

  fun dismissAppActions() {
    _selectedAppForAction.value = null
  }

  fun triggerDoubleTapGesture() {
    when (uiState.value.preferences.doubleTapAction) {
      DoubleTapAction.THEME_STUDIO -> openOverlay(LauncherOverlay.ThemeStudio)
      DoubleTapAction.QUICK_SEARCH -> openOverlay(LauncherOverlay.QuickSearch)
      DoubleTapAction.APP_DRAWER -> openOverlay(LauncherOverlay.AppDrawer)
      DoubleTapAction.NONE -> Unit
    }
  }

  fun togglePinToHome(app: InstalledApp) {
    val prefs = uiState.value.preferences
    val maxHomeItems = prefs.gridSize.columns * prefs.gridSize.homeRows
    viewModelScope.launch { prefsRepository.togglePinHome(app.componentKey, maxHomeItems) }
  }

  fun togglePinToDock(app: InstalledApp) {
    viewModelScope.launch { prefsRepository.togglePinDock(app.componentKey, maxDockItems = 5) }
  }

  fun toggleHideApp(app: InstalledApp) {
    viewModelScope.launch { prefsRepository.toggleHidden(app.componentKey) }
  }

  fun openAppInfo(app: InstalledApp) {
    appsRepository.openAppInfo(app.packageName)
  }

  fun openHomeSettings() {
    appsRepository.openHomeSettings()
  }

  fun openWifiSettings() {
    appsRepository.openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
  }

  fun openDisplaySettings() {
    appsRepository.openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
  }

  fun openDeviceSettings() {
    appsRepository.openSystemSettings(Settings.ACTION_SETTINGS)
  }

  fun setAccent(accent: PixelThemeAccent) {
    viewModelScope.launch { prefsRepository.setAccent(accent) }
  }

  fun setSurfaceMode(mode: PixelSurfaceMode) {
    viewModelScope.launch { prefsRepository.setSurfaceMode(mode) }
  }

  fun setWallpaper(wallpaper: PixelWallpaperStyle) {
    viewModelScope.launch { prefsRepository.setWallpaper(wallpaper) }
  }

  fun setIconShape(shape: PixelIconShape) {
    viewModelScope.launch { prefsRepository.setIconShape(shape) }
  }

  fun setIconPackMode(mode: PixelIconPackMode) {
    viewModelScope.launch { prefsRepository.setIconPackMode(mode) }
  }

  fun setNavBarStyle(style: PixelNavBarStyle) {
    viewModelScope.launch { prefsRepository.setNavBarStyle(style) }
  }

  fun setGridSize(gridSize: LauncherGridSize) {
    viewModelScope.launch { prefsRepository.setGridSize(gridSize) }
  }

  fun setPerformanceMode(mode: PerformanceMode) {
    viewModelScope.launch {
      prefsRepository.setPerformanceMode(mode)
      refreshTelemetry(mode)
    }
  }

  fun setDoubleTapAction(action: DoubleTapAction) {
    viewModelScope.launch { prefsRepository.setDoubleTapAction(action) }
  }

  fun setPixelAccentIconFrame(enabled: Boolean) {
    viewModelScope.launch { prefsRepository.setPixelAccentIconFrame(enabled) }
  }

  fun setShowHomeLabels(enabled: Boolean) {
    viewModelScope.launch { prefsRepository.setShowHomeLabels(enabled) }
  }

  fun setShowRamChip(enabled: Boolean) {
    viewModelScope.launch { prefsRepository.setShowRamChip(enabled) }
  }

  fun clearIconCache() {
    appsRepository.trimIconCache()
    refreshTelemetryOnly()
  }
}
