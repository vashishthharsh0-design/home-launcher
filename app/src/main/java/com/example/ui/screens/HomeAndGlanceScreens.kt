package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.InstalledApp
import com.example.ui.LauncherUiState
import com.example.ui.components.LowMemPixelAppTile
import com.example.ui.components.PixelAtAGlanceHeader
import com.example.ui.components.PixelSearchPill
import kotlin.math.ceil

@Composable
fun HomescreenPage(
  uiState: LauncherUiState,
  loadIcon: suspend (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  getCachedIcon: (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  onLaunchApp: (InstalledApp) -> Unit,
  onLongPressApp: (InstalledApp) -> Unit,
  onOpenDrawer: () -> Unit,
  onOpenQuickSearch: () -> Unit,
  onOpenThemeStudio: () -> Unit,
  onDoubleTap: () -> Unit,
  onVerticalDragDelta: (Float) -> Unit,
  onVerticalDragEnd: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = uiState.preferences
  val liteMode = uiState.telemetry.effectiveLiteMode
  val labelColor =
    if (prefs.surfaceMode.isDark || prefs.wallpaper.isDarkFriendly) {
      Color(0xFFF3F5FA)
    } else {
      Color(0xFF141824)
    }

  var accumulatedDrag by remember { mutableFloatStateOf(0f) }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .pointerInput(prefs.doubleTapAction) {
          detectTapGestures(onDoubleTap = { onDoubleTap() })
        }
        .pointerInput(Unit) {
          detectVerticalDragGestures(
            onDragStart = { accumulatedDrag = 0f },
            onVerticalDrag = { _, dragAmount ->
              accumulatedDrag += dragAmount
              onVerticalDragDelta(accumulatedDrag)
            },
            onDragEnd = {
              onVerticalDragEnd(accumulatedDrag)
              accumulatedDrag = 0f
            },
            onDragCancel = {
              onVerticalDragEnd(0f)
              accumulatedDrag = 0f
            },
          )
        },
    contentAlignment = Alignment.TopCenter,
  ) {
    val isExpandedWidth = maxWidth >= 600.dp
    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = if (isExpandedWidth) 620.dp else maxWidth)
          .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Pixel At-a-Glance Widget
      PixelAtAGlanceHeader(
        telemetry = uiState.telemetry,
        preferences = prefs,
        onOpenThemeStudio = onOpenThemeStudio,
        onOpenQuickSearch = onOpenQuickSearch,
      )

      // Center Pinned Homescreen Grid
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (uiState.homeApps.isEmpty() && !uiState.isLoading) {
          EmptyHomePrompt(onOpenDrawer = onOpenDrawer, labelColor = labelColor)
        } else {
          val cols = prefs.gridSize.columns
          val maxSlots = cols * prefs.gridSize.homeRows
          val visibleHomeApps = uiState.homeApps.take(maxSlots)
          val rows = ceil(visibleHomeApps.size / cols.toFloat()).toInt().coerceAtLeast(1)

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            for (r in 0 until rows) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                for (c in 0 until cols) {
                  val idx = r * cols + c
                  val app = visibleHomeApps.getOrNull(idx)
                  if (app != null) {
                    LowMemPixelAppTile(
                      app = app,
                      iconShape = prefs.iconShape,
                      iconPackMode = prefs.iconPackMode,
                      showLabel = prefs.showHomeLabels,
                      accentFrame = prefs.pixelAccentIconFrame,
                      liteMode = liteMode,
                      loadIcon = loadIcon,
                      getCachedIcon = getCachedIcon,
                      onClick = { onLaunchApp(app) },
                      onLongClick = { onLongPressApp(app) },
                      iconSize = if (cols >= 5) 44.dp else 52.dp,
                      labelColor = labelColor,
                      modifier = Modifier.weight(1f),
                    )
                  } else {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }
      }

      // Bottom Search Pill + Favorite Dock
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PixelSearchPill(
          onClick = onOpenQuickSearch,
          onOpenDrawer = onOpenDrawer,
        )

        if (uiState.dockApps.isNotEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (liteMode) 0.94f else 0.84f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("home_dock_bar"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              uiState.dockApps.take(5).forEach { app ->
                LowMemPixelAppTile(
                  app = app,
                  iconShape = prefs.iconShape,
                  iconPackMode = prefs.iconPackMode,
                  showLabel = false,
                  accentFrame = prefs.pixelAccentIconFrame,
                  liteMode = liteMode,
                  loadIcon = loadIcon,
                  getCachedIcon = getCachedIcon,
                  onClick = { onLaunchApp(app) },
                  onLongClick = { onLongPressApp(app) },
                  iconSize = 48.dp,
                  labelColor = labelColor,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyHomePrompt(onOpenDrawer: () -> Unit, labelColor: Color) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    modifier = Modifier.padding(16.dp),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "NO PINNED HOME APPS",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = "Swipe up or tap below to open the App Drawer, then long-press any app to pin it here.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(onClick = onOpenDrawer, modifier = Modifier.testTag("empty_open_drawer_btn")) {
        Text("Browse Apps")
      }
    }
  }
}

@Composable
fun QuickGlanceHubPage(
  uiState: LauncherUiState,
  onOpenThemeStudio: () -> Unit,
  onOpenHomeSettings: () -> Unit,
  onOpenWifiSettings: () -> Unit,
  onOpenDisplaySettings: () -> Unit,
  onOpenDeviceSettings: () -> Unit,
  onClearIconCache: () -> Unit,
  onRefreshApps: () -> Unit,
  onReturnHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val telemetry = uiState.telemetry
  val prefs = uiState.preferences

  Box(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 620.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "hub_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "PIXELTHEME HUB",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "${prefs.accent.displayName} • ${prefs.surfaceMode.displayName}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = onReturnHome,
            modifier = Modifier.testTag("hub_return_home_btn"),
          ) {
            Text("Home")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Back to Homescreen",
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      if (!uiState.isDefaultLauncher) {
        item(key = "default_launcher_card") {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                  text = "SET DEFAULT LAUNCHER",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "Make PixelTheme your system Home screen.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
              }
              Button(
                onClick = onOpenHomeSettings,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                modifier = Modifier.testTag("set_default_launcher_btn"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Home,
                  contentDescription = stringResource(R.string.cd_set_default_launcher),
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set")
              }
            }
          }
        }
      }

      item(key = "telemetry_card") {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Memory,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "DEVICE HEALTH & RAM",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = if (telemetry.effectiveLiteMode) "LITE ACTIVE" else "STANDARD",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            val usedRamMb = (telemetry.totalRamMb - telemetry.availRamMb).coerceAtLeast(0L)
            val ramProgress =
              if (telemetry.totalRamMb > 0L) (usedRamMb.toFloat() / telemetry.totalRamMb.toFloat()).coerceIn(0f, 1f)
              else 0f

            LinearProgressIndicator(
              progress = { ramProgress },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              TelemetryStat("Free RAM", "${telemetry.availRamMb} MB / ${telemetry.totalRamMb} MB")
              TelemetryStat("Free Storage", String.format("%.1f GB", telemetry.freeStorageGb))
              TelemetryStat("Cached Icons", "${telemetry.cachedIconCount}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onClearIconCache,
                modifier = Modifier.weight(1f).testTag("trim_cache_btn"),
              ) {
                Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Trim Cache", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              OutlinedButton(
                onClick = onRefreshApps,
                modifier = Modifier.weight(1f).testTag("rescan_apps_btn"),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rescan Apps", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }

      item(key = "quick_shortcuts_card") {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "PIXELTHEME & SYSTEM",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              QuickActionTile(
                icon = Icons.Filled.Palette,
                label = "Theme Studio",
                onClick = onOpenThemeStudio,
                modifier = Modifier.weight(1f).testTag("hub_theme_studio_tile"),
              )
              QuickActionTile(
                icon = Icons.Filled.Wifi,
                label = "Wi-Fi",
                onClick = onOpenWifiSettings,
                modifier = Modifier.weight(1f).testTag("hub_wifi_tile"),
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              QuickActionTile(
                icon = Icons.Filled.Brightness6,
                label = "Display",
                onClick = onOpenDisplaySettings,
                modifier = Modifier.weight(1f).testTag("hub_display_tile"),
              )
              QuickActionTile(
                icon = Icons.Filled.Settings,
                label = "Settings",
                onClick = onOpenDeviceSettings,
                modifier = Modifier.weight(1f).testTag("hub_settings_tile"),
              )
            }
          }
        }
      }

      item(key = "gestures_card") {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "GESTURE CONTROLS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text("• Swipe Up on Home → App Drawer", style = MaterialTheme.typography.bodyMedium)
            Text("• Swipe Down on Home → Quick Search & Math", style = MaterialTheme.typography.bodyMedium)
            Text("• Double-Tap Home → ${prefs.doubleTapAction.displayName}", style = MaterialTheme.typography.bodyMedium)
            Text("• Long-Press any Icon → Pin to Home / Dock / Hide", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun QuickActionTile(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    shape = RoundedCornerShape(12.dp),
    modifier =
      modifier
        .height(54.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
