package com.example

import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.ui.LauncherViewModel
import com.example.ui.PixelLauncherRoot

class MainActivity : ComponentActivity() {

  private val launcherViewModel: LauncherViewModel by viewModels()
  private var homePressSignal by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PixelLauncherRoot(
        viewModel = launcherViewModel,
        homePressSignal = homePressSignal,
      )
    }
  }

  override fun onResume() {
    super.onResume()
    launcherViewModel.refreshTelemetryOnly()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.hasCategory(Intent.CATEGORY_HOME)) {
      homePressSignal++
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
      launcherViewModel.clearIconCache()
    }
  }
}
