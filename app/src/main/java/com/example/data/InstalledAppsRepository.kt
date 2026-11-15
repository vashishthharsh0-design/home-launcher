package com.example.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.DeviceTelemetry
import com.example.model.InstalledApp
import com.example.model.PerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries real installed launchable apps via PackageManager and manages a memory-bounded
 * icon cache specifically tuned to prevent OOM on low-RAM / older budget handsets.
 */
class InstalledAppsRepository(private val context: Context) {

  private val packageManager: PackageManager = context.packageManager
  private val activityManager: ActivityManager =
    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  // Bound icon cache to <= 4 MiB on low-RAM devices or <= 8 MiB on standard devices
  private val maxCacheBytes: Int = run {
    val isLowRam = activityManager.isLowRamDevice || activityManager.memoryClass <= 128
    if (isLowRam) 4 * 1024 * 1024 else 8 * 1024 * 1024
  }

  private val iconCache =
    object : LruCache<String, ImageBitmap>(maxCacheBytes) {
      override fun sizeOf(key: String, value: ImageBitmap): Int {
        // Approximate byte footprint: width * height * 4 bytes (ARGB_8888)
        return (value.width * value.height * 4).coerceAtLeast(1024)
      }
    }

  suspend fun loadInstalledApps(): List<InstalledApp> =
    withContext(Dispatchers.IO) {
      val intent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolved: List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
          )
        } else {
          @Suppress("DEPRECATION")
          packageManager.queryIntentActivities(intent, 0)
        }

      val selfPackage = context.packageName
      val seenKeys = HashSet<String>(resolved.size)

      resolved
        .asSequence()
        .mapNotNull { info ->
          val activityInfo = info.activityInfo ?: return@mapNotNull null
          val pkg = activityInfo.packageName ?: return@mapNotNull null
          val act = activityInfo.name ?: return@mapNotNull null
          if (pkg == selfPackage) return@mapNotNull null
          val key = "$pkg/$act"
          if (!seenKeys.add(key)) return@mapNotNull null

          val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
          val isSystem =
            (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

          InstalledApp(
            packageName = pkg,
            activityName = act,
            label = label.ifEmpty { pkg.substringAfterLast('.') },
            isSystemApp = isSystem,
          )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
    }

  suspend fun getOrLoadIcon(
    app: InstalledApp,
    liteMode: Boolean,
    pixelize: Boolean = false,
  ): ImageBitmap? =
    withContext(Dispatchers.IO) {
      val modeTag =
        when {
          pixelize -> "px20"
          liteMode -> "lite"
          else -> "std"
        }
      val cacheKey = "${app.componentKey}@$modeTag"
      iconCache.get(cacheKey)?.let { return@withContext it }

      val drawable =
        try {
          val component = ComponentName(app.packageName, app.activityName)
          packageManager.getActivityIcon(component)
        } catch (_: PackageManager.NameNotFoundException) {
          try {
            packageManager.getApplicationIcon(app.packageName)
          } catch (_: PackageManager.NameNotFoundException) {
            null
          }
        } ?: return@withContext null

      val targetPx =
        when {
          pixelize -> 20
          liteMode -> 84
          else -> 120
        }
      val imageBitmap = drawableToScaledImageBitmap(drawable, targetPx)
      iconCache.put(cacheKey, imageBitmap)
      imageBitmap
    }

  fun getCachedIconImmediate(
    app: InstalledApp,
    liteMode: Boolean,
    pixelize: Boolean = false,
  ): ImageBitmap? {
    val modeTag =
      when {
        pixelize -> "px20"
        liteMode -> "lite"
        else -> "std"
      }
    val cacheKey = "${app.componentKey}@$modeTag"
    return iconCache.get(cacheKey)
  }

  fun trimIconCache() {
    iconCache.evictAll()
  }

  fun readDeviceTelemetry(performanceMode: PerformanceMode): DeviceTelemetry {
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)

    val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
    val availRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
    val hardwareLowRam = activityManager.isLowRamDevice || totalRamMb <= 2560L

    val effectiveLite =
      when (performanceMode) {
        PerformanceMode.AUTO -> hardwareLowRam
        PerformanceMode.LITE_ALWAYS -> true
        PerformanceMode.FULL_MOTION -> false
      }

    val batteryStatus =
      context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val batteryPct =
      if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100

    val statFs = StatFs(Environment.getDataDirectory().path)
    val freeBytes = statFs.availableBytes
    val freeGb = freeBytes / (1024f * 1024f * 1024f)

    return DeviceTelemetry(
      totalRamMb = totalRamMb,
      availRamMb = availRamMb,
      isLowRamDevice = hardwareLowRam,
      batteryPercent = batteryPct,
      isCharging = isCharging,
      freeStorageGb = freeGb,
      cachedIconCount = iconCache.size(),
      effectiveLiteMode = effectiveLite,
    )
  }

  fun isDefaultLauncher(): Boolean {
    val intent =
      Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolveInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
          intent,
          PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
      } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      }
    return resolveInfo?.activityInfo?.packageName == context.packageName
  }

  fun launchApp(app: InstalledApp): Boolean {
    return try {
      val launchIntent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          component = ComponentName(app.packageName, app.activityName)
          flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
      context.startActivity(launchIntent)
      true
    } catch (_: Exception) {
      val fallback = packageManager.getLaunchIntentForPackage(app.packageName)
      if (fallback != null) {
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallback)
        true
      } else {
        false
      }
    }
  }

  fun openAppInfo(packageName: String) {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }
  }

  fun openHomeSettings() {
    val intent =
      Intent(Settings.ACTION_HOME_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { context.startActivity(intent) }
      .onFailure { openSystemSettings(Settings.ACTION_SETTINGS) }
  }

  fun openSystemSettings(action: String = Settings.ACTION_SETTINGS) {
    val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { context.startActivity(intent) }
  }

  private fun drawableToScaledImageBitmap(drawable: Drawable, targetPx: Int): ImageBitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      val src = drawable.bitmap
      if (src.width <= targetPx && src.height <= targetPx) {
        return src.asImageBitmap()
      }
      val scaled = Bitmap.createScaledBitmap(src, targetPx, targetPx, true)
      return scaled.asImageBitmap()
    }

    val bitmap = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, targetPx, targetPx)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
  }
}
