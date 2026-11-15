package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.InstalledApp
import com.example.model.PixelIconPackMode
import com.example.model.PixelIconShape
import com.example.ui.theme.LocalPixelExtendedColors

/**
 * High-performance, low-memory AppGrid component for Jetpack Compose.
 *
 * Key low-end hardware optimizations:
 * - Flat single-pass drawBehind tile borders & pixel accents (no Surface elevation shadows or blur).
 * - Stable LazyVerticalGrid keys + contentType for fast item recycling on budget CPUs.
 * - Supports zero-bitmap 12x12 custom pixel-art Canvas sprites (CUSTOM_PIXEL_ART) as well as
 *   ultra-small 20x20 nearest-neighbor pixelized bitmaps (PIXELIZED_SYSTEM).
 */
@Composable
fun AppGrid(
  apps: List<InstalledApp>,
  columns: Int,
  iconShape: PixelIconShape,
  iconPackMode: PixelIconPackMode,
  showLabels: Boolean,
  accentFrame: Boolean,
  liteMode: Boolean,
  loadIcon: suspend (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  getCachedIcon: (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  onAppClick: (InstalledApp) -> Unit,
  onAppLongClick: (InstalledApp) -> Unit,
  modifier: Modifier = Modifier,
  gridState: LazyGridState = rememberLazyGridState(),
  contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
  labelColor: Color = MaterialTheme.colorScheme.onBackground,
  leadingContent: (LazyGridScope.() -> Unit)? = null,
  emptyContent: (@Composable () -> Unit)? = null,
) {
  val gridCells = remember(columns) { GridCells.Fixed(columns.coerceIn(2, 6)) }
  val iconSize = if (columns >= 5) 44.dp else 50.dp

  LazyVerticalGrid(
    state = gridState,
    columns = gridCells,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.fillMaxWidth().testTag("pixel_app_grid"),
  ) {
    leadingContent?.invoke(this)

    if (apps.isEmpty() && emptyContent != null) {
      item(span = { GridItemSpan(maxLineSpan) }, key = "app_grid_empty") {
        emptyContent()
      }
    } else {
      items(
        items = apps,
        key = { it.componentKey },
        contentType = { "pixel_app_grid_tile" },
      ) { app ->
        LowMemPixelAppTile(
          app = app,
          iconShape = iconShape,
          iconPackMode = iconPackMode,
          showLabel = showLabels,
          accentFrame = accentFrame,
          liteMode = liteMode,
          loadIcon = loadIcon,
          getCachedIcon = getCachedIcon,
          onClick = { onAppClick(app) },
          onLongClick = { onAppLongClick(app) },
          iconSize = iconSize,
          labelColor = labelColor,
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LowMemPixelAppTile(
  app: InstalledApp,
  iconShape: PixelIconShape,
  iconPackMode: PixelIconPackMode,
  showLabel: Boolean,
  accentFrame: Boolean,
  liteMode: Boolean,
  loadIcon: suspend (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  getCachedIcon: (InstalledApp, Boolean, Boolean) -> ImageBitmap?,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = 50.dp,
  labelColor: Color = MaterialTheme.colorScheme.onBackground,
) {
  val pixelizeBitmap = iconPackMode == PixelIconPackMode.PIXELIZED_SYSTEM
  val shouldLoadSystemBitmap = iconPackMode != PixelIconPackMode.CUSTOM_PIXEL_ART

  val cachedImmediate =
    remember(app.componentKey, liteMode, iconPackMode) {
      if (shouldLoadSystemBitmap) getCachedIcon(app, liteMode, pixelizeBitmap) else null
    }

  val iconBitmap by
    produceState(
      initialValue = cachedImmediate,
      key1 = app.componentKey,
      key2 = liteMode,
      key3 = iconPackMode,
    ) {
      value =
        if (!shouldLoadSystemBitmap) {
          null
        } else {
          cachedImmediate ?: loadIcon(app, liteMode, pixelizeBitmap)
        }
    }

  val tileShape: Shape =
    when (iconShape) {
      PixelIconShape.EIGHT_BIT_TILE -> RoundedCornerShape(4.dp)
      PixelIconShape.PIXEL_SQUIRCLE -> RoundedCornerShape(14.dp)
      PixelIconShape.RETRO_BEVEL -> RoundedCornerShape(6.dp)
      PixelIconShape.CLEAN_CIRCLE -> CircleShape
    }

  val extended = LocalPixelExtendedColors.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val tileFillColor = extended.pixelTileFill.copy(alpha = if (liteMode) 0.94f else 0.80f)
  val borderColor =
    if (accentFrame) primaryColor.copy(alpha = 0.72f) else extended.pixelFrameBorder
  val badgeColor = extended.pixelBadgeSecondary

  Column(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 4.dp, vertical = 5.dp)
        .testTag("app_tile_${app.packageName}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.size(iconSize)
          .clip(tileShape)
          .drawBehind {
            val strokePx = 1.5.dp.toPx()
            when (iconShape) {
              PixelIconShape.CLEAN_CIRCLE -> {
                drawCircle(color = tileFillColor)
                drawCircle(color = borderColor, style = Stroke(width = strokePx))
              }
              PixelIconShape.PIXEL_SQUIRCLE -> {
                val radius = 14.dp.toPx()
                drawRoundRect(
                  color = tileFillColor,
                  cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                  color = borderColor,
                  cornerRadius = CornerRadius(radius, radius),
                  style = Stroke(width = strokePx),
                )
              }
              PixelIconShape.RETRO_BEVEL -> {
                val radius = 6.dp.toPx()
                drawRoundRect(
                  color = tileFillColor,
                  cornerRadius = CornerRadius(radius, radius),
                )
                // Top & left bright bevel
                drawLine(
                  color = primaryColor.copy(alpha = 0.88f),
                  start = Offset(0f, strokePx),
                  end = Offset(size.width, strokePx),
                  strokeWidth = strokePx,
                )
                drawLine(
                  color = primaryColor.copy(alpha = 0.88f),
                  start = Offset(strokePx, 0f),
                  end = Offset(strokePx, size.height),
                  strokeWidth = strokePx,
                )
                // Bottom & right dark bevel
                drawLine(
                  color = Color.Black.copy(alpha = 0.55f),
                  start = Offset(0f, size.height - strokePx),
                  end = Offset(size.width, size.height - strokePx),
                  strokeWidth = strokePx,
                )
                drawLine(
                  color = Color.Black.copy(alpha = 0.55f),
                  start = Offset(size.width - strokePx, 0f),
                  end = Offset(size.width - strokePx, size.height),
                  strokeWidth = strokePx,
                )
              }
              PixelIconShape.EIGHT_BIT_TILE -> {
                val radius = 4.dp.toPx()
                drawRoundRect(
                  color = tileFillColor,
                  cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                  color = borderColor,
                  cornerRadius = CornerRadius(radius, radius),
                  style = Stroke(width = strokePx),
                )
                if (accentFrame) {
                  val notch = 5.dp.toPx()
                  drawRect(
                    color = badgeColor,
                    topLeft = Offset(size.width - notch - 3.dp.toPx(), 3.dp.toPx()),
                    size = Size(notch, notch),
                  )
                }
              }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      when (iconPackMode) {
        PixelIconPackMode.CUSTOM_PIXEL_ART -> {
          PixelArtAppSprite(
            app = app,
            modifier = Modifier.fillMaxSize().padding(4.dp),
          )
        }

        PixelIconPackMode.PIXELIZED_SYSTEM,
        PixelIconPackMode.CLASSIC_FRAMED -> {
          val bmp = iconBitmap
          if (bmp != null) {
            Image(
              bitmap = bmp,
              contentDescription = stringResource(R.string.cd_app_icon, app.label),
              modifier =
                Modifier.fillMaxSize()
                  .padding(if (iconShape == PixelIconShape.CLEAN_CIRCLE) 4.dp else 6.dp),
              filterQuality =
                if (pixelizeBitmap || liteMode) FilterQuality.None else FilterQuality.Low,
            )
          } else {
            PixelArtAppSprite(
              app = app,
              modifier = Modifier.fillMaxSize().padding(4.dp),
            )
          }
        }
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = app.label,
        style = MaterialTheme.typography.labelMedium,
        color = labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}
