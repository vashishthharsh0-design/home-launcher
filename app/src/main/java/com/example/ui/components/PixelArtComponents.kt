package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceTelemetry
import com.example.model.InstalledApp
import com.example.model.LauncherPreferences
import com.example.model.PixelIconShape
import com.example.model.PixelNavBarStyle
import com.example.model.PixelWallpaperStyle
import com.example.ui.theme.LocalPixelExtendedColors
import com.example.ui.theme.PixelSurfaceMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Renders either a memory-efficient downsampled pixel-art drawable or a zero-bitmap
 * procedural Canvas pixel matrix grid for ultra-low-RAM handsets.
 */
@Composable
fun PixelWallpaperBackdrop(
  wallpaper: PixelWallpaperStyle,
  surfaceMode: PixelSurfaceMode,
  liteMode: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val primary = MaterialTheme.colorScheme.primary
  val background = MaterialTheme.colorScheme.background

  if (wallpaper == PixelWallpaperStyle.ZERO_RAM_GRID) {
    PixelMatrixGridCanvas(
      baseColor = background,
      accentColor = primary,
      isDark = surfaceMode.isDark,
      modifier = modifier,
    )
    return
  }

  val resId =
    when (wallpaper) {
      PixelWallpaperStyle.PIXEL_HORIZON -> R.drawable.img_wallpaper_pixel_horizon
      PixelWallpaperStyle.RETRO_NIGHT -> R.drawable.img_wallpaper_retro_night
      PixelWallpaperStyle.PASTEL_SKY -> R.drawable.img_wallpaper_pastel_sky
      PixelWallpaperStyle.ZERO_RAM_GRID -> 0
    }

  val wallpaperBitmap by
    produceState<ImageBitmap?>(initialValue = null, key1 = resId, key2 = liteMode) {
      value = decodeSampledWallpaper(context, resId, liteMode)
    }

  Box(modifier = modifier.fillMaxSize().background(background)) {
    val bmp = wallpaperBitmap
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        filterQuality = if (liteMode) FilterQuality.None else FilterQuality.Low,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // Readability scrim tailored to surface mode; solid flat alpha in Lite mode avoids extra gradient passes
    val scrimColor =
      when {
        surfaceMode == PixelSurfaceMode.AMOLED_BLACK -> Color(0x99000000)
        surfaceMode.isDark -> Color(0x6B0D1017)
        else -> Color(0x38FFFFFF)
      }
    Box(modifier = Modifier.fillMaxSize().background(scrimColor))
  }
}

@Composable
private fun PixelMatrixGridCanvas(
  baseColor: Color,
  accentColor: Color,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxSize().background(baseColor)) {
    val step = 28.dp.toPx()
    val lineColor =
      if (isDark) accentColor.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.12f)
    val dotColor =
      if (isDark) accentColor.copy(alpha = 0.28f) else accentColor.copy(alpha = 0.24f)

    var x = 0f
    while (x <= size.width) {
      drawLine(color = lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
      x += step
    }
    var y = 0f
    while (y <= size.height) {
      drawLine(color = lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
      y += step
    }

    // Subtle pixel stars in upper third
    val block = 4.dp.toPx()
    val starOffsets =
      listOf(
        Offset(0.18f, 0.11f),
        Offset(0.42f, 0.08f),
        Offset(0.76f, 0.14f),
        Offset(0.86f, 0.23f),
        Offset(0.28f, 0.22f),
      )
    starOffsets.forEach { rel ->
      drawRect(
        color = dotColor,
        topLeft = Offset(size.width * rel.x, size.height * rel.y),
        size = Size(block, block),
      )
    }
  }
}

private suspend fun decodeSampledWallpaper(
  context: Context,
  resId: Int,
  liteMode: Boolean,
): ImageBitmap? =
  withContext(Dispatchers.IO) {
    runCatching {
        val options =
          BitmapFactory.Options().apply {
            inSampleSize = if (liteMode) 2 else 1
            inPreferredConfig =
              if (liteMode) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
          }
        BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
      }
      .getOrNull()
  }

@Composable
fun PixelAtAGlanceHeader(
  telemetry: DeviceTelemetry,
  preferences: LauncherPreferences,
  onOpenThemeStudio: () -> Unit,
  onOpenQuickSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      val nextMinuteDelay = 60_000L - (nowMillis % 60_000L)
      delay(nextMinuteDelay.coerceAtLeast(1_000L))
    }
  }

  val timeText = remember(nowMillis) {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
  }
  val dateText = remember(nowMillis) {
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
  }

  val extended = LocalPixelExtendedColors.current
  val highContrastText =
    if (preferences.surfaceMode.isDark || preferences.wallpaper.isDarkFriendly) {
      Color(0xFFF4F6FB)
    } else {
      Color(0xFF151821)
    }
  val subtleText = highContrastText.copy(alpha = 0.78f)

  Surface(
    color =
      if (telemetry.effectiveLiteMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
      } else {
        extended.scrimSurface.copy(alpha = 0.78f)
      },
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          PixelSunSprite(accent = MaterialTheme.colorScheme.primary, secondary = extended.pixelBadgeSecondary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = dateText.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = timeText,
          style = MaterialTheme.typography.displayLarge,
          color = highContrastText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Battery badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
          ) {
            Icon(
              imageVector =
                if (telemetry.isCharging) Icons.Filled.BatteryChargingFull
                else Icons.Filled.BatteryStd,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${telemetry.batteryPercent}%",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          if (preferences.showRamChip) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                  .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Memory,
                contentDescription = stringResource(R.string.cd_low_ram_badge),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              val modeTag = if (telemetry.effectiveLiteMode) "LITE" else "STD"
              Text(
                text = "$modeTag • ${telemetry.availRamMb}MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }

      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = onOpenThemeStudio,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .testTag("theme_studio_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = stringResource(R.string.cd_open_theme_studio),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
        Text(
          text = preferences.accent.displayName,
          style = MaterialTheme.typography.labelSmall,
          color = subtleText,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun PixelSunSprite(accent: Color, secondary: Color) {
  Canvas(modifier = Modifier.size(16.dp)) {
    val px = size.width / 8f
    val grid =
      listOf(
        "00111100",
        "01222210",
        "12222221",
        "12222221",
        "12222221",
        "12222221",
        "01222210",
        "00111100",
      )
    grid.forEachIndexed { row, line ->
      line.forEachIndexed { col, ch ->
        val color =
          when (ch) {
            '1' -> accent
            '2' -> secondary
            else -> null
          }
        if (color != null) {
          drawRect(
            color = color,
            topLeft = Offset(col * px, row * px),
            size = Size(px + 0.5f, px + 0.5f),
          )
        }
      }
    }
  }
}

@Composable
fun PixelSearchPill(
  onClick: () -> Unit,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalPixelExtendedColors.current
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    modifier =
      modifier
        .fillMaxWidth()
        .height(50.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag("home_search_pill"),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = stringResource(R.string.cd_open_quick_search),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.launcher_search_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      // 4-pixel Google/PixelTheme color dots
      Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.size(6.dp).background(extended.pixelBadgeSecondary))
        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFBBC04)))
      }
      Spacer(modifier = Modifier.width(6.dp))
      IconButton(
        onClick = onOpenDrawer,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_shortcut_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Apps,
          contentDescription = stringResource(R.string.cd_open_drawer),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

/**
 * Interactive launcher navigation strip honoring gcantoni/PixelTheme's 3 navigation bar styles
 * (Pixel Pill, AOSP Retro, Filled Bar) while respecting system WindowInsets.navigationBars.
 */
@Composable
fun PixelNavBarPill(
  navBarStyle: PixelNavBarStyle,
  currentPage: Int,
  onGoGlance: () -> Unit,
  onGoHome: () -> Unit,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    when (navBarStyle) {
      PixelNavBarStyle.PIXEL_PILL -> {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
          shape = RoundedCornerShape(24.dp),
          border =
            androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            ),
          modifier = Modifier.height(48.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            IconButton(
              onClick = onGoGlance,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_glance_btn"),
            ) {
              Icon(
                imageVector = Icons.Filled.Dashboard,
                contentDescription = "System Hub",
                tint =
                  if (currentPage == 0) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
            // Iconic Pixel pill handle
            Box(
              modifier =
                Modifier.width(52.dp)
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(
                    if (currentPage == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  )
                  .clickable(onClick = onGoHome)
                  .testTag("nav_home_pill")
            )
            IconButton(
              onClick = onOpenDrawer,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_drawer_btn"),
            ) {
              Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = stringResource(R.string.cd_open_drawer),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      PixelNavBarStyle.AOSP_RETRO -> {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onGoGlance,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_aosp_left"),
          ) {
            // Pixel-art triangle glyph
            Canvas(modifier = Modifier.size(16.dp)) {
              drawRect(
                color =
                  if (currentPage == 0) Color(0xFF4285F4) else Color(0xFFB3B9C9),
                topLeft = Offset(2.dp.toPx(), 6.dp.toPx()),
                size = Size(12.dp.toPx(), 4.dp.toPx()),
              )
            }
          }
          IconButton(
            onClick = onGoHome,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_aosp_home"),
          ) {
            Icon(
              imageVector = Icons.Filled.Home,
              contentDescription = "Home",
              tint =
                if (currentPage == 1) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_aosp_drawer"),
          ) {
            // Retro square glyph
            Box(
              modifier =
                Modifier.size(14.dp)
                  .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
      }

      PixelNavBarStyle.FILLED_BAR -> {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "HUB",
              style = MaterialTheme.typography.titleSmall,
              color =
                if (currentPage == 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onGoGlance)
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("nav_filled_hub"),
            )
            Box(
              modifier =
                Modifier.size(width = 48.dp, height = 8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(MaterialTheme.colorScheme.primary)
                  .clickable(onClick = onGoHome)
                  .testTag("nav_filled_home")
            )
            Text(
              text = "APPS",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onOpenDrawer)
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("nav_filled_apps"),
            )
          }
        }
      }
    }
  }
}
