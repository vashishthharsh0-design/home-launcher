package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DoubleTapAction
import com.example.model.InstalledApp
import com.example.model.LauncherGridSize
import com.example.model.PerformanceMode
import com.example.model.PixelIconPackMode
import com.example.model.PixelIconShape
import com.example.model.PixelNavBarStyle
import com.example.model.PixelWallpaperStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.AppGrid
import com.example.ui.components.LowMemPixelAppTile
import com.example.ui.theme.PixelSurfaceMode
import com.example.ui.theme.PixelThemeAccent
import kotlinx.coroutines.launch

@Composable
fun AppDrawerOverlay(
  uiState: LauncherUiState,
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  loadIcon: suspend (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  getCachedIcon: (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  onLaunchApp: (InstalledApp) -> Unit,
  onLongPressApp: (InstalledApp) -> Unit,
  onSetIconPackMode: (PixelIconPackMode) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = uiState.preferences
  val liteMode = uiState.telemetry.effectiveLiteMode
  val gridState = rememberLazyGridState()
  val scope = rememberCoroutineScope()

  val filteredApps =
    remember(uiState.visibleDrawerApps, searchQuery) {
      if (searchQuery.isBlank()) uiState.visibleDrawerApps
      else {
        val q = searchQuery.trim().lowercase()
        uiState.visibleDrawerApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
    }

  val alphabetLetters =
    remember(filteredApps) {
      filteredApps.mapNotNull { it.label.firstOrNull()?.uppercaseChar() }.distinct()
    }

  var dragDownAccum by remember { mutableFloatStateOf(0f) }

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = if (liteMode) 0.98f else 0.95f),
    modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      // Pull-down handle bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .pointerInput(Unit) {
              detectVerticalDragGestures(
                onDragStart = { dragDownAccum = 0f },
                onVerticalDrag = { _, dragAmount -> dragDownAccum += dragAmount },
                onDragEnd = {
                  if (dragDownAccum > 110f) onDismiss()
                  dragDownAccum = 0f
                },
              )
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.size(width = 44.dp, height = 5.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onQueryChange,
          singleLine = true,
          placeholder = { Text(stringResource(R.string.drawer_search_hint)) },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
              }
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("drawer_search_input"),
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_close_btn"),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_close_overlay),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Quick 1-tap pixel icon style switcher right inside the App Drawer
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      ) {
        items(PixelIconPackMode.entries) { mode ->
          FilterChip(
            selected = prefs.iconPackMode == mode,
            onClick = { onSetIconPackMode(mode) },
            label = { Text(mode.displayName) },
            modifier = Modifier.testTag("drawer_icon_mode_${mode.name}"),
          )
        }
      }

      Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        AppGrid(
          apps = filteredApps,
          columns = prefs.gridSize.columns,
          iconShape = prefs.iconShape,
          iconPackMode = prefs.iconPackMode,
          showLabels = true,
          accentFrame = prefs.pixelAccentIconFrame,
          liteMode = liteMode,
          loadIcon = loadIcon,
          getCachedIcon = getCachedIcon,
          onAppClick = onLaunchApp,
          onAppLongClick = onLongPressApp,
          gridState = gridState,
          modifier = Modifier.weight(1f).testTag("app_drawer_grid"),
          leadingContent = {
            if (searchQuery.isBlank() && uiState.recentApps.isNotEmpty()) {
              item(span = { GridItemSpan(maxLineSpan) }, key = "recent_header") {
                Text(
                  text = "RECENT",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
              }
              item(span = { GridItemSpan(maxLineSpan) }, key = "recent_strip") {
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                  items(
                    items = uiState.recentApps.take(5),
                    key = { "recent_${it.componentKey}" },
                  ) { app ->
                    LowMemPixelAppTile(
                      app = app,
                      iconShape = prefs.iconShape,
                      iconPackMode = prefs.iconPackMode,
                      showLabel = true,
                      accentFrame = prefs.pixelAccentIconFrame,
                      liteMode = liteMode,
                      loadIcon = loadIcon,
                      getCachedIcon = getCachedIcon,
                      onClick = { onLaunchApp(app) },
                      onLongClick = { onLongPressApp(app) },
                      iconSize = 46.dp,
                      modifier = Modifier.width(72.dp),
                    )
                  }
                }
              }
              item(span = { GridItemSpan(maxLineSpan) }, key = "all_apps_header") {
                Text(
                  text = "ALL APPS (${filteredApps.size})",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
                )
              }
            }
          },
          emptyContent = {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "No matching apps found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
        )

        // Fast A-Z letter jump rail (great on low-end devices to avoid heavy scrolling!)
        if (searchQuery.isBlank() && alphabetLetters.size > 4) {
          Column(
            modifier =
              Modifier.fillMaxHeight()
                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                .width(24.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            alphabetLetters.take(18).forEach { letter ->
              Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                  Modifier.clip(RoundedCornerShape(4.dp))
                    .clickable {
                      val index =
                        filteredApps.indexOfFirst {
                          it.label.firstOrNull()?.uppercaseChar() == letter
                        }
                      if (index >= 0) {
                        val headerOffset = if (uiState.recentApps.isNotEmpty()) 3 else 0
                        scope.launch { gridState.scrollToItem(index + headerOffset) }
                      }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun QuickSearchOverlay(
  uiState: LauncherUiState,
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  loadIcon: suspend (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  getCachedIcon: (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  onLaunchApp: (InstalledApp) -> Unit,
  onLongPressApp: (InstalledApp) -> Unit,
  onOpenThemeStudio: () -> Unit,
  onOpenDeviceSettings: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = uiState.preferences
  val liteMode = uiState.telemetry.effectiveLiteMode

  val matches =
    remember(uiState.visibleDrawerApps, searchQuery) {
      if (searchQuery.isBlank()) uiState.recentApps.ifEmpty { uiState.visibleDrawerApps.take(8) }
      else {
        val q = searchQuery.trim().lowercase()
        uiState.visibleDrawerApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(16)
      }
    }

  val mathResult = remember(searchQuery) { evaluateSimpleMath(searchQuery) }

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = if (liteMode) 0.98f else 0.94f),
    modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().widthIn(max = 620.dp).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "QUICK COMMAND",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_search_close_btn"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_overlay))
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Search apps or calculate (e.g. 24*7)…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
              Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_search_input"),
      )

      if (mathResult != null) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Calculate,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "= $mathResult",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = false,
          onClick = onOpenThemeStudio,
          label = { Text("PixelTheme Studio") },
          leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_open_theme_studio"),
        )
        FilterChip(
          selected = false,
          onClick = onOpenDeviceSettings,
          label = { Text("Device Settings") },
          leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_open_settings"),
        )
      }

      Text(
        text = if (searchQuery.isBlank()) "SUGGESTED & RECENT" else "MATCHES (${matches.size})",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )

      AppGrid(
        apps = matches,
        columns = 4,
        iconShape = prefs.iconShape,
        iconPackMode = prefs.iconPackMode,
        showLabels = true,
        accentFrame = prefs.pixelAccentIconFrame,
        liteMode = liteMode,
        loadIcon = loadIcon,
        getCachedIcon = getCachedIcon,
        onAppClick = onLaunchApp,
        onAppLongClick = onLongPressApp,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun ThemeStudioSheet(
  uiState: LauncherUiState,
  onSetAccent: (PixelThemeAccent) -> Unit,
  onSetSurfaceMode: (PixelSurfaceMode) -> Unit,
  onSetWallpaper: (PixelWallpaperStyle) -> Unit,
  onSetIconShape: (PixelIconShape) -> Unit,
  onSetIconPackMode: (PixelIconPackMode) -> Unit,
  onSetNavBarStyle: (PixelNavBarStyle) -> Unit,
  onSetGridSize: (LauncherGridSize) -> Unit,
  onSetPerformanceMode: (PerformanceMode) -> Unit,
  onSetDoubleTapAction: (DoubleTapAction) -> Unit,
  onSetAccentIconFrame: (Boolean) -> Unit,
  onSetShowHomeLabels: (Boolean) -> Unit,
  onSetShowRamChip: (Boolean) -> Unit,
  onUnhideApp: (InstalledApp) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = uiState.preferences

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "PIXELTHEME STUDIO",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Custom Pixel overlays & low-RAM engine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onDismiss, modifier = Modifier.testTag("theme_studio_done_btn")) {
          Text("Done")
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.weight(1f),
      ) {
        item(key = "accent_section") {
          StudioSection("1. PIXELTHEME ACCENT PALETTE") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(PixelThemeAccent.entries) { accent ->
                val selected = prefs.accent == accent
                val swatchColor =
                  if (prefs.surfaceMode.isDark) accent.primaryDark else accent.primaryLight
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier =
                    Modifier.clip(RoundedCornerShape(12.dp))
                      .clickable { onSetAccent(accent) }
                      .padding(6.dp)
                      .testTag("accent_swatch_${accent.name}"),
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                          if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                          else Modifier
                        )
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = accent.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        item(key = "surface_section") {
          StudioSection("2. SURFACE THEME") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              PixelSurfaceMode.entries.forEach { mode ->
                FilterChip(
                  selected = prefs.surfaceMode == mode,
                  onClick = { onSetSurfaceMode(mode) },
                  label = { Text(mode.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  modifier = Modifier.weight(1f).testTag("surface_mode_${mode.name}"),
                )
              }
            }
          }
        }

        item(key = "wallpaper_section") {
          StudioSection("3. PIXEL-ART BACKDROP") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(PixelWallpaperStyle.entries) { wp ->
                FilterChip(
                  selected = prefs.wallpaper == wp,
                  onClick = { onSetWallpaper(wp) },
                  label = { Text(wp.displayName) },
                  modifier = Modifier.testTag("wallpaper_${wp.name}"),
                )
              }
            }
          }
        }

        item(key = "icon_shape_section") {
          StudioSection("4. PIXEL ICON FRAME") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(PixelIconShape.entries) { shape ->
                FilterChip(
                  selected = prefs.iconShape == shape,
                  onClick = { onSetIconShape(shape) },
                  label = { Text(shape.displayName) },
                  modifier = Modifier.testTag("icon_shape_${shape.name}"),
                )
              }
            }
          }
        }

        item(key = "icon_pack_section") {
          StudioSection("4B. ICON RENDERER MODE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              PixelIconPackMode.entries.forEach { mode ->
                val selected = prefs.iconPackMode == mode
                Surface(
                  color =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(12.dp),
                  border =
                    if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null,
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onSetIconPackMode(mode) }
                      .testTag("icon_pack_${mode.name}"),
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                      text = mode.displayName,
                      style = MaterialTheme.typography.labelLarge,
                      color =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = mode.subtitle,
                      style = MaterialTheme.typography.bodyMedium,
                      color =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        item(key = "navbar_section") {
          StudioSection("5. NAVIGATION BAR STYLE (PIXELTHEME)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              PixelNavBarStyle.entries.forEach { style ->
                FilterChip(
                  selected = prefs.navBarStyle == style,
                  onClick = { onSetNavBarStyle(style) },
                  label = { Text(style.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  modifier = Modifier.weight(1f).testTag("navbar_style_${style.name}"),
                )
              }
            }
          }
        }

        item(key = "grid_section") {
          StudioSection("6. HOMESCREEN GRID") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              LauncherGridSize.entries.forEach { grid ->
                FilterChip(
                  selected = prefs.gridSize == grid,
                  onClick = { onSetGridSize(grid) },
                  label = { Text(grid.displayName) },
                  modifier = Modifier.weight(1f).testTag("grid_${grid.name}"),
                )
              }
            }
          }
        }

        item(key = "performance_section") {
          StudioSection("7. LOW-END DEVICE PERFORMANCE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              PerformanceMode.entries.forEach { mode ->
                val selected = prefs.performanceMode == mode
                Surface(
                  color =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(12.dp),
                  border =
                    if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null,
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onSetPerformanceMode(mode) }
                      .testTag("perf_mode_${mode.name}"),
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                      text = mode.displayName,
                      style = MaterialTheme.typography.labelLarge,
                      color =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = mode.description,
                      style = MaterialTheme.typography.bodyMedium,
                      color =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        item(key = "gesture_section") {
          StudioSection("8. DOUBLE-TAP HOME GESTURE") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(DoubleTapAction.entries) { action ->
                FilterChip(
                  selected = prefs.doubleTapAction == action,
                  onClick = { onSetDoubleTapAction(action) },
                  label = { Text(action.displayName) },
                  modifier = Modifier.testTag("double_tap_${action.name}"),
                )
              }
            }
          }
        }

        item(key = "toggles_section") {
          StudioSection("9. DISPLAY OPTIONS") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              StudioToggleRow(
                title = "Accent Icon Borders",
                subtitle = "Pixel-styled accent rims around app tiles",
                checked = prefs.pixelAccentIconFrame,
                onCheckedChange = onSetAccentIconFrame,
              )
              StudioToggleRow(
                title = "Show Home App Labels",
                subtitle = "Display app names under pinned homescreen icons",
                checked = prefs.showHomeLabels,
                onCheckedChange = onSetShowHomeLabels,
              )
              StudioToggleRow(
                title = "Show RAM / Mode Chip",
                subtitle = "Show live available RAM in At-a-Glance widget",
                checked = prefs.showRamChip,
                onCheckedChange = onSetShowRamChip,
              )
            }
          }
        }

        if (uiState.hiddenApps.isNotEmpty()) {
          item(key = "hidden_apps_section") {
            StudioSection("10. HIDDEN APPS (${uiState.hiddenApps.size})") {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.hiddenApps.forEach { app ->
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                      )
                      TextButton(onClick = { onUnhideApp(app) }) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unhide")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StudioSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    content()
  }
}

@Composable
private fun StudioToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .clickable { onCheckedChange(!checked) }
        .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.labelLarge)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun AppActionDialog(
  app: InstalledApp,
  isPinnedHome: Boolean,
  isPinnedDock: Boolean,
  onLaunch: () -> Unit,
  onTogglePinHome: () -> Unit,
  onTogglePinDock: () -> Unit,
  onOpenAppInfo: () -> Unit,
  onHideApp: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = app.label,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = app.packageName,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        DialogActionRow(
          icon = Icons.Filled.Home,
          label = if (isPinnedHome) "Unpin from Homescreen" else stringResource(R.string.cd_pin_home),
          onClick = {
            onTogglePinHome()
            onDismiss()
          },
        )
        DialogActionRow(
          icon = Icons.Filled.Dock,
          label = if (isPinnedDock) "Remove from Dock" else stringResource(R.string.cd_pin_dock),
          onClick = {
            onTogglePinDock()
            onDismiss()
          },
        )
        DialogActionRow(
          icon = Icons.Filled.Info,
          label = stringResource(R.string.cd_app_info),
          onClick = {
            onOpenAppInfo()
            onDismiss()
          },
        )
        DialogActionRow(
          icon = Icons.Filled.VisibilityOff,
          label = "Hide from App Drawer",
          onClick = {
            onHideApp()
            onDismiss()
          },
        )
      }
    },
    confirmButton = {
      Button(onClick = {
        onDismiss()
        onLaunch()
      }) {
        Text("Open")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
private fun DialogActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = label, style = MaterialTheme.typography.bodyLarge)
  }
}

/** Lightweight zero-dependency arithmetic evaluator for Quick Search (supports +, -, *, /). */
private fun evaluateSimpleMath(raw: String): String? {
  val clean = raw.replace(" ", "")
  if (clean.isEmpty() || !clean.any { it in "+-*/" }) return null
  if (!clean.all { it.isDigit() || it in "+-*/." }) return null

  return runCatching {
      val tokens = Regex("([0-9]+(\\.[0-9]+)?)|[+\\-*/]").findAll(clean).map { it.value }.toList()
      if (tokens.size < 3) return null
      val nums = mutableListOf<Double>()
      val ops = mutableListOf<String>()
      nums.add(tokens[0].toDouble())
      var i = 1
      while (i + 1 < tokens.size) {
        val op = tokens[i]
        val next = tokens[i + 1].toDouble()
        if (op == "*" || op == "/") {
          val prev = nums.removeAt(nums.lastIndex)
          nums.add(if (op == "*") prev * next else if (next == 0.0) return null else prev / next)
        } else {
          ops.add(op)
          nums.add(next)
        }
        i += 2
      }
      var result = nums[0]
      ops.forEachIndexed { idx, op ->
        result = if (op == "+") result + nums[idx + 1] else result - nums[idx + 1]
      }
      if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result).trimEnd('0').trimEnd('.')
    }
    .getOrNull()
}
