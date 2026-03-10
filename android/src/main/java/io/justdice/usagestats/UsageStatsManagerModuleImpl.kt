package io.justdice.usagestats

import android.annotation.TargetApi
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.EventStats
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.RemoteException
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.common.MapBuilder
import io.justdice.usagestats.model.AppData
import io.justdice.usagestats.utils.UsageUtils
import io.justdice.usagestats.utils.UsageUtils.humanReadableMillis

/**
 * Shared implementation class containing all native logic.
 * Not a React module itself — logic is exposed through the arch-specific wrappers.
 */
class UsageStatsManagerModuleImpl(private val reactContext: ReactApplicationContext) {

  @RequiresApi(Build.VERSION_CODES.M)
  private var networkStatsManager: NetworkStatsManager? =
      reactContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

  fun getConstants(): Map<String, Any>? {
    val constants: MutableMap<String, Any> = MapBuilder.newHashMap()
    constants["INTERVAL_WEEKLY"] = UsageStatsManager.INTERVAL_WEEKLY
    constants["INTERVAL_MONTHLY"] = UsageStatsManager.INTERVAL_MONTHLY
    constants["INTERVAL_YEARLY"] = UsageStatsManager.INTERVAL_YEARLY
    constants["INTERVAL_DAILY"] = UsageStatsManager.INTERVAL_DAILY
    constants["INTERVAL_BEST"] = UsageStatsManager.INTERVAL_BEST
    constants["TYPE_WIFI"] = ConnectivityManager.TYPE_WIFI
    constants["TYPE_MOBILE"] = ConnectivityManager.TYPE_MOBILE
    constants["TYPE_MOBILE_AND_WIFI"] = Int.MAX_VALUE
    return constants
  }

  private fun packageExists(packageName: String): Boolean {
    return try {
      reactContext.packageManager.getApplicationInfo(packageName, 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }
  }

  fun showUsageAccessSettings(packageName: String) {
    val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
    if (packageExists(packageName)) {
      intent.data = Uri.fromParts("package", packageName, null)
    }
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    reactContext.startActivity(intent)
  }

  // interval is Double to match codegen spec (TurboModules codegen uses Double for numeric types)
  fun queryUsageStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    val packageManager: PackageManager = reactContext.packageManager
    val result: WritableNativeArray = WritableNativeArray()
    val usageStatsManager =
        reactContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: List<UsageStats> =
        usageStatsManager.queryUsageStats(interval.toInt(), startTime.toLong(), endTime.toLong())

    for (us in queryUsageStats) {
      // On API 29+ totalTimeInForeground is always 0 due to OS restrictions.
      // Use totalTimeVisible (API 29+) as the primary metric, fall back to totalTimeInForeground.
      val rawTime: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        us.totalTimeVisible
      } else {
        us.totalTimeInForeground
      }

      if (rawTime > 0) {
        val usageStats: WritableMap = WritableNativeMap()
        usageStats.putString("packageName", us.packageName)
        val totalTimeInSeconds = rawTime / 1000.0
        usageStats.putDouble("totalTimeInForeground", totalTimeInSeconds)
        usageStats.putDouble("totalTimeVisible", rawTime.toDouble())
        usageStats.putDouble("firstTimeStamp", us.firstTimeStamp.toDouble())
        usageStats.putDouble("lastTimeStamp", us.lastTimeStamp.toDouble())
        usageStats.putDouble("lastTimeUsed", us.lastTimeUsed.toDouble())
        usageStats.putBoolean("isSystem", UsageUtils.isSystemApp(packageManager, us.packageName))
        usageStats.putString(
            "appName",
            UsageUtils.parsePackageName(packageManager, us.packageName.toString()).toString()
        )
        result.pushMap(usageStats)
      }
    }
    promise.resolve(result)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
  fun queryAndAggregateUsageStats(startTime: Double, endTime: Double, promise: Promise) {
    val packageManager: PackageManager = reactContext.packageManager
    val result: WritableNativeArray = WritableNativeArray()
    val usageStatsManager =
        reactContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: MutableMap<String, UsageStats>? =
        usageStatsManager.queryAndAggregateUsageStats(startTime.toLong(), endTime.toLong())

    if (queryUsageStats != null) {
      for (us in queryUsageStats.values) {
        // On API 29+ totalTimeInForeground is always 0 due to OS restrictions.
        val rawTime: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          us.totalTimeVisible
        } else {
          us.totalTimeInForeground
        }

        if (rawTime > 0) {
          val usageStats: WritableMap = WritableNativeMap()
          usageStats.putString("packageName", us.packageName)
          val totalTimeInSeconds = rawTime.toDouble() / 1000
          usageStats.putDouble("totalTimeInForeground", totalTimeInSeconds)
          usageStats.putDouble("totalTimeVisible", rawTime.toDouble())
          usageStats.putDouble("firstTimeStamp", us.firstTimeStamp.toDouble())
          usageStats.putDouble("lastTimeStamp", us.lastTimeStamp.toDouble())
          usageStats.putDouble("lastTimeUsed", us.lastTimeUsed.toDouble())
          usageStats.putInt("describeContents", us.describeContents())
          usageStats.putBoolean("isSystem", UsageUtils.isSystemApp(packageManager, us.packageName.toString()))
          usageStats.putString(
              "appName",
              UsageUtils.parsePackageName(packageManager, us.packageName.toString()).toString()
          )
          result.pushMap(usageStats)
        }
      }
    }
    promise.resolve(result)
  }

  fun writeToWritableMap(mutableList: MutableList<AppData>): WritableMap {
    val writableMap: WritableMap = WritableNativeMap()
    for ((index, appData) in mutableList.withIndex()) {
      val appDataMap: WritableMap = WritableNativeMap()
      appDataMap.putString("name", appData.mName)
      appDataMap.putString("packageName", appData.mPackageName)
      appDataMap.putDouble("eventTime", appData.mEventTime.toDouble())
      appDataMap.putDouble("usageTime", appData.mUsageTime.toDouble())
      appDataMap.putString("humanReadableUsageTime", humanReadableMillis(appData.mUsageTime))
      appDataMap.putInt("eventType", appData.mEventType)
      appDataMap.putInt("count", appData.mCount)
      appDataMap.putBoolean("isSystem", appData.mIsSystem)
      writableMap.putMap(index.toString(), appDataMap)
    }
    return writableMap
  }

  fun queryEvents(startTime: Double, endTime: Double, promise: Promise) {
    val result = WritableNativeArray()
    val manager =
        reactContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events: UsageEvents = manager.queryEvents(startTime.toLong(), endTime.toLong())
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
      events.getNextEvent(event)
      val _event: WritableMap = WritableNativeMap()
      _event.putInt("eventType", event.eventType)
      _event.putDouble("timeStamp", event.timeStamp.toDouble())
      _event.putString("packageName", event.packageName)
      result.pushMap(_event)
    }
    promise.resolve(result)
  }

  // interval is Double to match codegen spec
  @RequiresApi(Build.VERSION_CODES.P)
  fun queryEventsStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    val result: WritableMap = WritableNativeMap()
    val usageStatsManager =
        reactContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: MutableList<EventStats>? =
        usageStatsManager.queryEventStats(interval.toInt(), startTime.toLong(), endTime.toLong())
    if (queryUsageStats != null) {
      for (us in queryUsageStats) {
        val usageStats: WritableMap = WritableNativeMap()
        usageStats.putDouble("firstTimeStamp", us.firstTimeStamp.toDouble())
        usageStats.putDouble("lastTimeStamp", us.lastTimeStamp.toDouble())
        usageStats.putDouble("lastTimeUsed", us.totalTime.toDouble())
        usageStats.putInt("describeContents", us.describeContents())
        result.putMap(us.eventType.toString(), usageStats)
      }
    }
    promise.resolve(result)
  }

  fun checkForPermission(promise: Promise) {
    val appOps =
        reactContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode: Int =
        appOps.checkOpNoThrow(
            OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            reactContext.packageName
        )
    promise.resolve(mode == MODE_ALLOWED)
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun getDataUsage(
      networkType: Int,
      subscriberId: String?,
      packageUid: Int,
      startTime: Long,
      endTime: Long
  ): Double {
    var currentDataUsage = 0.0
    try {
      val networkStatsByApp =
          networkStatsManager?.querySummary(networkType, subscriberId, startTime, endTime)!!
      do {
        val bucket = NetworkStats.Bucket()
        networkStatsByApp.getNextBucket(bucket)
        if (bucket.uid == packageUid) {
          currentDataUsage += bucket.rxBytes + bucket.txBytes
        }
      } while (networkStatsByApp.hasNextBucket())
    } catch (e: RemoteException) {
      e.printStackTrace()
    }
    return currentDataUsage
  }

  // networkType is Double to match codegen spec
  fun getAppDataUsage(
      packageName: String,
      networkType: Double,
      startTime: Double,
      endTime: Double,
      promise: Promise
  ) {
    val uid = getAppUid(packageName)
    val netType = networkType.toInt()
    when {
      netType == ConnectivityManager.TYPE_MOBILE -> promise.resolve(
          getDataUsage(ConnectivityManager.TYPE_MOBILE, null, uid, startTime.toLong(), endTime.toLong())
      )
      netType == ConnectivityManager.TYPE_WIFI -> promise.resolve(
          getDataUsage(ConnectivityManager.TYPE_WIFI, "", uid, startTime.toLong(), endTime.toLong())
      )
      else -> promise.resolve(
          getDataUsage(ConnectivityManager.TYPE_MOBILE, "", uid, startTime.toLong(), endTime.toLong()) +
          getDataUsage(ConnectivityManager.TYPE_WIFI, "", uid, startTime.toLong(), endTime.toLong())
      )
    }
  }

  private fun getAppUid(packageName: String): Int {
    return try {
      reactContext.packageManager.getApplicationInfo(packageName, 0).uid
    } catch (e: PackageManager.NameNotFoundException) {
      0
    }
  }

  companion object {
    const val MODULE_NAME = "JustDiceReactNativeUsageStats"
  }
}

internal class ClonedEvent(event: UsageEvents.Event) {
  var packageName: String
  var eventClass: String
  var timeStamp: Long
  var eventType: Int

  init {
    packageName = event.packageName
    eventClass = event.className
    timeStamp = event.timeStamp
    eventType = event.eventType
  }
}



