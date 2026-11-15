package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LauncherOverlay
import com.example.ui.components.PixelNavBarPill
import com.example.ui.components.PixelWallpaperBackdrop
import com.example.ui.screens.AppActionDialog
import com.example.ui.screens.AppDrawerOverlay
import com.example.ui.screens.HomescreenPage
import com.example.ui.screens.QuickGlanceHubPage
import com.example.ui.screens.QuickSearchOverlay
import com.example.ui.screens.ThemeStudioSheet
import com.example.ui.theme.PixelThemeWrapper
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun PixelLauncherRoot(
  viewModel: LauncherViewModel,
  homePressSignal: Int,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val overlay by viewModel.overlay.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedApp by viewModel.selectedAppForAction.collectAsStateWithLifecycle()

  val prefs = uiState.preferences
  val liteMode = uiState.telemetry.effectiveLiteMode
  val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
  val scope = rememberCoroutineScope()

  // Live vertical gesture feedback bar while swiping up/down on Homescreen
  var activeVerticalDrag by remember { mutableFloatStateOf(0f) }

  // Return cleanly to Center Homescreen when system HOME button is pressed
  LaunchedEffect(homePressSignal) {
    if (homePressSignal > 0) {
      viewModel.openOverlay(LauncherOverlay.None)
      if (pagerState.currentPage != 1) {
        if (liteMode) pagerState.scrollToPage(1) else pagerState.animateScrollToPage(1)
      }
    }
  }

  BackHandler(enabled = overlay != LauncherOverlay.None || pagerState.currentPage != 1) {
    if (overlay != LauncherOverlay.None) {
      viewModel.openOverlay(LauncherOverlay.None)
    } else if (pagerState.currentPage != 1) {
      scope.launch {
        if (liteMode) pagerState.scrollToPage(1) else pagerState.animateScrollToPage(1)
      }
    }
  }

  PixelThemeWrapper(
    accent = prefs.accent,
    surfaceMode = prefs.surfaceMode,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Wallpaper Backdrop
      PixelWallpaperBackdrop(
        wallpaper = prefs.wallpaper,
        surfaceMode = prefs.surfaceMode,
        liteMode = liteMode,
      )

      Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
          state = pagerState,
          beyondViewportPageCount = 0,
          userScrollEnabled = overlay == LauncherOverlay.None,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
          when (page) {
            0 ->
              QuickGlanceHubPage(
                uiState = uiState,
                onOpenThemeStudio = { viewModel.openOverlay(LauncherOverlay.ThemeStudio) },
                onOpenHomeSettings = { viewModel.openHomeSettings() },
                onOpenWifiSettings = { viewModel.openWifiSettings() },
                onOpenDisplaySettings = { viewModel.openDisplaySettings() },
                onOpenDeviceSettings = { viewModel.openDeviceSettings() },
                onClearIconCache = { viewModel.clearIconCache() },
                onRefreshApps = { viewModel.refreshAll() },
                onReturnHome = {
                  scope.launch {
                    if (liteMode) pagerState.scrollToPage(1)
                    else pagerState.animateScrollToPage(1)
                  }
                },
              )

            1 ->
              HomescreenPage(
                uiState = uiState,
                loadIcon = viewModel::loadAppIcon,
                getCachedIcon = viewModel::getCachedAppIcon,
                onLaunchApp = viewModel::launchApp,
                onLongPressApp = viewModel::openAppActions,
                onOpenDrawer = { viewModel.openOverlay(LauncherOverlay.AppDrawer) },
                onOpenQuickSearch = { viewModel.openOverlay(LauncherOverlay.QuickSearch) },
                onOpenThemeStudio = { viewModel.openOverlay(LauncherOverlay.ThemeStudio) },
                onDoubleTap = { viewModel.triggerDoubleTapGesture() },
                onVerticalDragDelta = { delta -> activeVerticalDrag = delta },
                onVerticalDragEnd = { totalDelta ->
                  activeVerticalDrag = 0f
                  when {
                    totalDelta < -110f -> viewModel.openOverlay(LauncherOverlay.AppDrawer)
                    totalDelta > 110f -> viewModel.openOverlay(LauncherOverlay.QuickSearch)
                  }
                },
              )
          }
        }

        // PixelTheme Navigation Strip (Pixel Pill / AOSP Retro / Filled Bar)
        PixelNavBarPill(
          navBarStyle = prefs.navBarStyle,
          currentPage = pagerState.currentPage,
          onGoGlance = {
            scope.launch {
              if (liteMode) pagerState.scrollToPage(0) else pagerState.animateScrollToPage(0)
            }
          },
          onGoHome = {
            scope.launch {
              if (liteMode) pagerState.scrollToPage(1) else pagerState.animateScrollToPage(1)
            }
          },
          onOpenDrawer = { viewModel.openOverlay(LauncherOverlay.AppDrawer) },
        )
      }

      // Subtle tactile gesture progress pill when user pulls up or down on Homescreen
      if (abs(activeVerticalDrag) > 24f && overlay == LauncherOverlay.None) {
        val progressFraction = (abs(activeVerticalDrag) / 140f).coerceIn(0.15f, 1f)
        val align = if (activeVerticalDrag < 0f) Alignment.BottomCenter else Alignment.TopCenter
        Box(
          modifier =
            Modifier.align(align)
              .padding(vertical = 84.dp)
              .fillMaxWidth(0.32f * progressFraction)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(MaterialTheme.colorScheme.primary)
        )
      }

      // App Drawer Overlay (Swipe Up)
      AnimatedVisibility(
        visible = overlay == LauncherOverlay.AppDrawer,
        enter =
          if (liteMode) {
            slideInVertically(animationSpec = tween(130, easing = FastOutSlowInEasing)) { it / 3 } +
              fadeIn(tween(110))
          } else {
            slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } +
              fadeIn(tween(180))
          },
        exit =
          if (liteMode) {
            slideOutVertically(animationSpec = tween(110)) { it / 3 } + fadeOut(tween(100))
          } else {
            slideOutVertically(animationSpec = tween(200)) { it / 2 } + fadeOut(tween(160))
          },
      ) {
        AppDrawerOverlay(
          uiState = uiState,
          searchQuery = searchQuery,
          onQueryChange = viewModel::updateSearchQuery,
          loadIcon = viewModel::loadAppIcon,
          getCachedIcon = viewModel::getCachedAppIcon,
          onLaunchApp = viewModel::launchApp,
          onLongPressApp = viewModel::openAppActions,
          onSetIconPackMode = viewModel::setIconPackMode,
          onDismiss = { viewModel.openOverlay(LauncherOverlay.None) },
        )
      }

      // Quick Search & Command Overlay (Swipe Down)
      AnimatedVisibility(
        visible = overlay == LauncherOverlay.QuickSearch,
        enter =
          if (liteMode) {
            fadeIn(tween(100))
          } else {
            slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it / 4 } +
              fadeIn(tween(160))
          },
        exit =
          if (liteMode) {
            fadeOut(tween(90))
          } else {
            slideOutVertically(animationSpec = tween(180)) { -it / 4 } + fadeOut(tween(140))
          },
      ) {
        QuickSearchOverlay(
          uiState = uiState,
          searchQuery = searchQuery,
          onQueryChange = viewModel::updateSearchQuery,
          loadIcon = viewModel::loadAppIcon,
          getCachedIcon = viewModel::getCachedAppIcon,
          onLaunchApp = viewModel::launchApp,
          onLongPressApp = viewModel::openAppActions,
          onOpenThemeStudio = { viewModel.openOverlay(LauncherOverlay.ThemeStudio) },
          onOpenDeviceSettings = { viewModel.openDeviceSettings() },
          onDismiss = { viewModel.openOverlay(LauncherOverlay.None) },
        )
      }

      // PixelTheme Studio Overlay
      AnimatedVisibility(
        visible = overlay == LauncherOverlay.ThemeStudio,
        enter =
          if (liteMode) fadeIn(tween(110))
          else slideInVertically(animationSpec = tween(220)) { it / 4 } + fadeIn(tween(180)),
        exit =
          if (liteMode) fadeOut(tween(90))
          else slideOutVertically(animationSpec = tween(180)) { it / 4 } + fadeOut(tween(150)),
      ) {
        ThemeStudioSheet(
          uiState = uiState,
          onSetAccent = viewModel::setAccent,
          onSetSurfaceMode = viewModel::setSurfaceMode,
          onSetWallpaper = viewModel::setWallpaper,
          onSetIconShape = viewModel::setIconShape,
          onSetIconPackMode = viewModel::setIconPackMode,
          onSetNavBarStyle = viewModel::setNavBarStyle,
          onSetGridSize = viewModel::setGridSize,
          onSetPerformanceMode = viewModel::setPerformanceMode,
          onSetDoubleTapAction = viewModel::setDoubleTapAction,
          onSetAccentIconFrame = viewModel::setPixelAccentIconFrame,
          onSetShowHomeLabels = viewModel::setShowHomeLabels,
          onSetShowRamChip = viewModel::setShowRamChip,
          onUnhideApp = viewModel::toggleHideApp,
          onDismiss = { viewModel.openOverlay(LauncherOverlay.None) },
        )
      }

      // Long-Press App Action Dialog
      val appForAction = selectedApp
      if (appForAction != null) {
        AppActionDialog(
          app = appForAction,
          isPinnedHome = appForAction.componentKey in prefs.pinnedHomeKeys,
          isPinnedDock = appForAction.componentKey in prefs.pinnedDockKeys,
          onLaunch = { viewModel.launchApp(appForAction) },
          onTogglePinHome = { viewModel.togglePinToHome(appForAction) },
          onTogglePinDock = { viewModel.togglePinToDock(appForAction) },
          onOpenAppInfo = { viewModel.openAppInfo(appForAction) },
          onHideApp = { viewModel.toggleHideApp(appForAction) },
          onDismiss = viewModel::dismissAppActions,
        )
      }
    }
  }
}
