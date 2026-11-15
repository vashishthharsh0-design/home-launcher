package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.InstalledAppsRepository
import com.example.model.InstalledApp
import com.example.model.PerformanceMode
import com.example.model.PixelIconPackMode
import com.example.model.PixelIconShape
import com.example.ui.components.AppGrid
import com.example.ui.components.PixelAppCategory
import com.example.ui.components.resolvePixelAppCategory
import com.example.ui.theme.PixelThemeWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app name matches PixelTheme`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PixelTheme", appName)
  }

  @Test
  fun `lite performance mode activates when forced for low-end devices`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = InstalledAppsRepository(context)
    val telemetry = repo.readDeviceTelemetry(PerformanceMode.LITE_ALWAYS)
    assertTrue(telemetry.effectiveLiteMode)
    assertTrue(telemetry.totalRamMb >= 1L)
  }

  @Test
  fun `pixel sprite category resolves accurately without bitmaps`() {
    val cam = InstalledApp("com.android.camera2", "CameraActivity", "Camera", true)
    val calc = InstalledApp("com.android.calculator2", "Calculator", "Calculator", true)
    val custom = InstalledApp("org.example.retro", "MainActivity", "RetroApp", false)

    assertEquals(PixelAppCategory.CAMERA, resolvePixelAppCategory(cam))
    assertEquals(PixelAppCategory.CALCULATOR, resolvePixelAppCategory(calc))
    assertEquals(PixelAppCategory.GENERIC_APP, resolvePixelAppCategory(custom))
  }

  @Test
  fun `app grid renders custom pixel-art apps and handles clicks`() {
    val sampleApps =
      listOf(
        InstalledApp("com.android.dialer", "DialtactsActivity", "Phone", true),
        InstalledApp("com.android.settings", "Settings", "Settings", true),
        InstalledApp("com.example.notes", "NotesActivity", "Notes", false),
      )
    var clickedPkg: String? = null

    composeTestRule.setContent {
      PixelThemeWrapper {
        AppGrid(
          apps = sampleApps,
          columns = 3,
          iconShape = PixelIconShape.EIGHT_BIT_TILE,
          iconPackMode = PixelIconPackMode.CUSTOM_PIXEL_ART,
          showLabels = true,
          accentFrame = true,
          liteMode = true,
          loadIcon = { _, _, _ -> null },
          getCachedIcon = { _, _, _ -> null },
          onAppClick = { clickedPkg = it.packageName },
          onAppLongClick = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("pixel_app_grid").assertIsDisplayed()
    composeTestRule.onNodeWithText("Phone").assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_tile_com.android.settings").performClick()
    assertEquals("com.android.settings", clickedPkg)
  }

  @Test
  fun `main activity launches cleanly without crashing`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertTrue(activity != null && !activity.isFinishing)
  }
}
