package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.InstalledApp
import kotlin.math.abs

/**
 * Zero-bitmap-allocation 12x12 custom pixel-art sprites for common Android app categories,
 * plus deterministic 8-bit cartridge/gem sprites for any other installed application.
 * Designed for low-RAM devices: renders directly via integer grid rects on Compose Canvas.
 */
enum class PixelAppCategory(
  val primaryColor: Color,
  val secondaryColor: Color,
  val highlightColor: Color,
  val grid: List<String>,
) {
  PHONE(
    primaryColor = Color(0xFF34A853),
    secondaryColor = Color(0xFF1E6E34),
    highlightColor = Color(0xFFA8E6B8),
    grid =
      listOf(
        "............",
        "..111.......",
        ".13311......",
        ".1311.......",
        "..112.......",
        "...122......",
        "....122.....",
        ".....12211..",
        "......11331.",
        ".......1131.",
        "........111.",
        "............",
      ),
  ),
  MESSAGES(
    primaryColor = Color(0xFF4285F4),
    secondaryColor = Color(0xFF1E56B3),
    highlightColor = Color(0xFFD2E3FC),
    grid =
      listOf(
        "............",
        ".1111111111.",
        "133333333331",
        "111111111111",
        "113331133311",
        "111111111111",
        "113333331111",
        "122222222221",
        ".2211111111.",
        "..11........",
        "..1.........",
        "............",
      ),
  ),
  BROWSER(
    primaryColor = Color(0xFF26C6DA),
    secondaryColor = Color(0xFF1A73E8),
    highlightColor = Color(0xFFE0F7FA),
    grid =
      listOf(
        "............",
        "...111111...",
        "..13322331..",
        ".1322112231.",
        ".1211331121.",
        ".1213333121.",
        ".1213333121.",
        ".1211331121.",
        ".1322112231.",
        "..13322331..",
        "...111111...",
        "............",
      ),
  ),
  CAMERA(
    primaryColor = Color(0xFF78909C),
    secondaryColor = Color(0xFF37474F),
    highlightColor = Color(0xFF8AB4F8),
    grid =
      listOf(
        "............",
        "....1111....",
        "...133331...",
        ".1111111111.",
        "111222222111",
        "112233332211",
        "112331133211",
        "112331133211",
        "112233332211",
        "122222222221",
        ".2222222222.",
        "............",
      ),
  ),
  SETTINGS(
    primaryColor = Color(0xFF90A4AE),
    secondaryColor = Color(0xFF455A64),
    highlightColor = Color(0xFFECEFF1),
    grid =
      listOf(
        "............",
        "....1111....",
        "..1.1331.1..",
        ".1111221111.",
        "..12211221..",
        ".1121331211.",
        ".1121331211.",
        "..12211221..",
        ".1111221111.",
        "..1.1221.1..",
        "....1111....",
        "............",
      ),
  ),
  CLOCK(
    primaryColor = Color(0xFF5C6BC0),
    secondaryColor = Color(0xFF283593),
    highlightColor = Color(0xFFE8EAF6),
    grid =
      listOf(
        "............",
        "...111111...",
        "..13333331..",
        ".1333223331.",
        ".1333223331.",
        ".1333222231.",
        ".1333333331.",
        ".1333333331.",
        ".1233333321.",
        "..12222221..",
        "...111111...",
        "............",
      ),
  ),
  CALCULATOR(
    primaryColor = Color(0xFF00897B),
    secondaryColor = Color(0xFF004D40),
    highlightColor = Color(0xFFB2DFDB),
    grid =
      listOf(
        "............",
        "..11111111..",
        ".1222222221.",
        ".1233333321.",
        ".1222222221.",
        ".1231231231.",
        ".1222222221.",
        ".1231231231.",
        ".1222222221.",
        ".1231231331.",
        "..11111111..",
        "............",
      ),
  ),
  MUSIC(
    primaryColor = Color(0xFFEC407A),
    secondaryColor = Color(0xFFAD1457),
    highlightColor = Color(0xFFFCE4EC),
    grid =
      listOf(
        "............",
        ".....111111.",
        ".....133331.",
        ".....111111.",
        ".....1....1.",
        ".....1....1.",
        "...111..111.",
        "..1331.1331.",
        "..1311.1311.",
        "...11...11..",
        "............",
        "............",
      ),
  ),
  PHOTOS(
    primaryColor = Color(0xFFFBBC04),
    secondaryColor = Color(0xFF34A853),
    highlightColor = Color(0xFFFFF8E1),
    grid =
      listOf(
        "............",
        ".1111111111.",
        "133333333331",
        "133113333331",
        "131111333331",
        "133333322331",
        "133223222231",
        "132222222221",
        "122222222221",
        "122222222221",
        ".1111111111.",
        "............",
      ),
  ),
  CALENDAR(
    primaryColor = Color(0xFFEA4335),
    secondaryColor = Color(0xFFB71C1C),
    highlightColor = Color(0xFFFFFFFF),
    grid =
      listOf(
        "............",
        "..1......1..",
        ".1111111111.",
        ".1222222221.",
        ".1111111111.",
        ".1333333331.",
        ".1311331131.",
        ".1333333331.",
        ".1311331131.",
        ".1333333331.",
        "..11111111..",
        "............",
      ),
  ),
  FILES(
    primaryColor = Color(0xFFFFA726),
    secondaryColor = Color(0xFFEF6C00),
    highlightColor = Color(0xFFFFF3E0),
    grid =
      listOf(
        "............",
        "............",
        ".11111......",
        "13333311111.",
        "111111111111",
        "133333333331",
        "111111111111",
        "122222222221",
        "122222222221",
        "122222222221",
        ".2222222222.",
        "............",
      ),
  ),
  EMAIL(
    primaryColor = Color(0xFFE53935),
    secondaryColor = Color(0xFF1E88E5),
    highlightColor = Color(0xFFFFFFFF),
    grid =
      listOf(
        "............",
        "............",
        ".1111111111.",
        "131333333131",
        "133133331331",
        "133311113331",
        "133333333331",
        "123333333321",
        "122222222221",
        ".2222222222.",
        "............",
        "............",
      ),
  ),
  MAPS(
    primaryColor = Color(0xFF34A853),
    secondaryColor = Color(0xFF4285F4),
    highlightColor = Color(0xFFEA4335),
    grid =
      listOf(
        "............",
        "....3333....",
        "...331133...",
        "...311113...",
        "....3333....",
        "..11.33.22..",
        ".1111332222.",
        ".1112222221.",
        ".1222211111.",
        "..22111111..",
        "............",
        "............",
      ),
  ),
  STORE(
    primaryColor = Color(0xFF00ACC1),
    secondaryColor = Color(0xFF43A047),
    highlightColor = Color(0xFFFBBC04),
    grid =
      listOf(
        "............",
        "....1111....",
        "...1....1...",
        "..11111111..",
        "..12222221..",
        "..12332221..",
        "..12333321..",
        "..12332221..",
        "..12222221..",
        "..11111111..",
        "............",
        "............",
      ),
  ),
  NOTES(
    primaryColor = Color(0xFFFFCA28),
    secondaryColor = Color(0xFFF57F17),
    highlightColor = Color(0xFFFFFDE7),
    grid =
      listOf(
        "............",
        "..11111111..",
        "..13333331..",
        "..13222231..",
        "..13333331..",
        "..13222331..",
        "..13333331..",
        "..13223331..",
        "..13333111..",
        "..111111....",
        "............",
        "............",
      ),
  ),
  GENERIC_APP(
    primaryColor = Color(0xFF7E57C2),
    secondaryColor = Color(0xFF4527A0),
    highlightColor = Color(0xFFEDE7F6),
    grid =
      listOf(
        "............",
        "..11111111..",
        ".1333333331.",
        ".1311111131.",
        ".131....131.",
        ".131....131.",
        ".131....131.",
        ".1311111131.",
        ".1222222221.",
        "..22222222..",
        "............",
        "............",
      ),
  ),
}

private val fallbackPalettes =
  listOf(
    Triple(Color(0xFF4285F4), Color(0xFF1A438A), Color(0xFFD2E3FC)),
    Triple(Color(0xFF00897B), Color(0xFF004D40), Color(0xFFB2DFDB)),
    Triple(Color(0xFFAF6050), Color(0xFF6E2E24), Color(0xFFFFDAD4)),
    Triple(Color(0xFF9C27B0), Color(0xFF4A148C), Color(0xFFF3E5F5)),
    Triple(Color(0xFF5C6BC0), Color(0xFF283593), Color(0xFFE8EAF6)),
    Triple(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFFC8E6C9)),
  )

fun resolvePixelAppCategory(app: InstalledApp): PixelAppCategory {
  val key = "${app.packageName.lowercase()} ${app.label.lowercase()}"
  return when {
    key.contains("dialer") || key.contains("phone") || key.contains("telecom") ->
      PixelAppCategory.PHONE
    key.contains("messaging") || key.contains("mms") || key.contains("sms") ||
      key.contains("chat") || key.contains("whatsapp") -> PixelAppCategory.MESSAGES
    key.contains("chrome") || key.contains("browser") || key.contains("webview") ||
      key.contains("firefox") -> PixelAppCategory.BROWSER
    key.contains("camera") -> PixelAppCategory.CAMERA
    key.contains("settings") -> PixelAppCategory.SETTINGS
    key.contains("deskclock") || key.contains("clock") || key.contains("alarm") ->
      PixelAppCategory.CLOCK
    key.contains("calculator") -> PixelAppCategory.CALCULATOR
    key.contains("music") || key.contains("audio") || key.contains("spotify") ||
      key.contains("youtube") -> PixelAppCategory.MUSIC
    key.contains("gallery") || key.contains("photos") || key.contains("image") ->
      PixelAppCategory.PHOTOS
    key.contains("calendar") -> PixelAppCategory.CALENDAR
    key.contains("documentsui") || key.contains("files") || key.contains("file") ->
      PixelAppCategory.FILES
    key.contains("mail") || key.contains("gmail") -> PixelAppCategory.EMAIL
    key.contains("maps") || key.contains("navigation") -> PixelAppCategory.MAPS
    key.contains("vending") || key.contains("play store") || key.contains("store") ->
      PixelAppCategory.STORE
    key.contains("note") || key.contains("keep") || key.contains("docs") ->
      PixelAppCategory.NOTES
    else -> PixelAppCategory.GENERIC_APP
  }
}

/**
 * Renders a crisp 12x12 pixel-art Canvas sprite for the given [InstalledApp].
 * Allocates zero Bitmaps, making it ideal for low-memory budget devices.
 */
@Composable
fun PixelArtAppSprite(
  app: InstalledApp,
  modifier: Modifier = Modifier,
) {
  val category = remember(app.componentKey) { resolvePixelAppCategory(app) }
  val palette =
    remember(app.componentKey, category) {
      if (category != PixelAppCategory.GENERIC_APP) {
        Triple(category.primaryColor, category.secondaryColor, category.highlightColor)
      } else {
        fallbackPalettes[abs(app.packageName.hashCode()) % fallbackPalettes.size]
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
      val rows = category.grid.size
      val cols = category.grid[0].length
      val cellW = size.width / cols
      val cellH = size.height / rows

      for (r in 0 until rows) {
        val line = category.grid[r]
        for (c in 0 until cols) {
          val color =
            when (line[c]) {
              '1' -> palette.first
              '2' -> palette.second
              '3' -> palette.third
              else -> null
            }
          if (color != null) {
            drawRect(
              color = color,
              topLeft = Offset(c * cellW, r * cellH),
              size = Size(cellW + 0.6f, cellH + 0.6f),
            )
          }
        }
      }
    }

    if (category == PixelAppCategory.GENERIC_APP) {
      Text(
        text = app.label.take(1).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.third,
      )
    }
  }
}
