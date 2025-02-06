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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.RemoteException
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.common.MapBuilder
import io.justdice.usagestats.model.AppData
import io.justdice.usagestats.utils.SortOrder
import io.justdice.usagestats.utils.UsageUtils
import io.justdice.usagestats.utils.UsageUtils.getAppUid
import io.justdice.usagestats.utils.UsageUtils.getTimeRange
import io.justdice.usagestats.utils.UsageUtils.humanReadableMillis
import io.justdice.usagestats.utils.UsageUtils.isSystemApp
import io.justdice.usagestats.utils.UsageUtils.parsePackageName


class UsageStatsManagerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
  private var reactContext: Context = reactContext

  @RequiresApi(Build.VERSION_CODES.M)
  private var networkStatsManager: NetworkStatsManager? =
      reactContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): kotlin.collections.Map<String, Any>? {
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
    val packageManager: PackageManager? = reactContext?.getPackageManager()
    var info: ApplicationInfo? = null
    info =
        try {
          packageManager?.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
          e.printStackTrace()
          return false
        }
    return true
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  fun showUsageAccessSettings(packageName: String) {
    val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
    if (packageExists(packageName)) {
      intent.setData(Uri.fromParts("package", packageName, null))
    }
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    reactContext?.startActivity(intent)
  }

  @ReactMethod
  fun queryUsageStats(interval: Int, startTime: Double, endTime: Double, promise: Promise) {
    val packageManager: PackageManager = reactContext!!.packageManager
    val result: WritableMap = WritableNativeMap()
    val usageStatsManager: UsageStatsManager =
        reactContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: List<UsageStats> =
        usageStatsManager.queryUsageStats(interval, startTime.toLong(), endTime.toLong())

    for (us in queryUsageStats) {
      if (us.totalTimeInForeground > 0) {
        val usageStats: WritableMap = WritableNativeMap()
        usageStats.putString("packageName", us.packageName)
        val totalTimeInSeconds = us.totalTimeInForeground / 1000
        usageStats.putDouble("totalTimeInForeground", totalTimeInSeconds.toDouble())
        usageStats.putDouble("firstTimeStamp", us.firstTimeStamp.toDouble())
        usageStats.putDouble("lastTimeStamp", us.lastTimeStamp.toDouble())
        usageStats.putDouble("lastTimeUsed", us.lastTimeUsed.toDouble())
        usageStats.putBoolean("isSystem", isSystemApp(us.packageName))
        usageStats.putString(
            "appName",
            UsageUtils.parsePackageName(packageManager, us.packageName.toString()).toString()
        )
        result.putMap(us.packageName, usageStats)
      }
    }

    promise.resolve(result)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
  @ReactMethod
  fun queryAndAggregateUsageStats(startTime: Double, endTime: Double, promise: Promise) {
    val packageManager: PackageManager = reactContext!!.packageManager
    val result: WritableMap = WritableNativeMap()
    val usageStatsManager: UsageStatsManager =
        reactContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: MutableMap<String, UsageStats>? =
        usageStatsManager.queryAndAggregateUsageStats(startTime.toLong(), endTime.toLong())

    if (queryUsageStats != null) {
      for (us in queryUsageStats.values) {
        if (us.totalTimeInForeground.toInt() != 0) {
          val usageStats: WritableMap = WritableNativeMap()
          usageStats.putString("packageName", us.packageName)
          val totalTimeInSeconds = us.totalTimeInForeground.toDouble() / 1000
          usageStats.putDouble("totalTimeInForeground", totalTimeInSeconds)
          usageStats.putDouble("firstTimeStamp", us.firstTimeStamp.toDouble())
          usageStats.putDouble("lastTimeStamp", us.lastTimeStamp.toDouble())
          usageStats.putDouble("lastTimeUsed", us.lastTimeUsed.toDouble())
          usageStats.putInt("describeContents", us.describeContents())
          usageStats.putBoolean("isSystem", isSystemApp(us.packageName.toString()))
          usageStats.putString(
              "appName",
              UsageUtils.parsePackageName(packageManager, us.packageName.toString()).toString()
          )
          result.putMap(us.packageName, usageStats)
        }
      }
    }
    promise.resolve(result)
  }

  fun writeToWritableMap(mutableList: MutableList<AppData>): WritableMap {
    val writableMap: WritableMap = WritableNativeMap()

    for ((index, appData) in mutableList.withIndex()) {
      val appDataMap: WritableMap =  WritableNativeMap()
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

  private fun containItem(items: List<AppData>, packageName: String): AppData? {
    for (item in items) {
      if (item.mPackageName == packageName) return item
    }
    return null
  }

  @ReactMethod
  fun queryEvents(startTime: Double, endTime: Double, promise: Promise) {
    val result = WritableNativeArray()

    val manager: UsageStatsManager =
        reactContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

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

  @RequiresApi(Build.VERSION_CODES.P)
  @ReactMethod
  fun queryEventsStats(interval: Int, startTime: Double, endTime: Double, promise: Promise) {

    val result: WritableMap = WritableNativeMap()
    val usageStatsManager: UsageStatsManager =
        reactContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val queryUsageStats: MutableList<EventStats>? =
        usageStatsManager.queryEventStats(interval, startTime.toLong(), endTime.toLong())
    val us = UsageEvents.Event()
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

  private fun isSystemApp(packageName: String): Boolean {
    var isSys: Boolean = false
    try {
      val packageManager: PackageManager? = reactContext?.packageManager
      val appInfo = packageManager?.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
      if (appInfo != null && appInfo.flags == ApplicationInfo.FLAG_SYSTEM) {
        isSys = true
      }
      return isSys
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
      return isSys
    }
  }

  private fun getAppNameFromPackage(packageName: String, context: Context): String {
    val packageManager = context.packageManager
    var p = packageName

    try {
      val mainIntent = Intent(Intent.ACTION_MAIN, null)
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
      val packages = packageManager.queryIntentActivities(mainIntent, 0)

      val app_name_list = ArrayList<String>()
      val app_package_list = ArrayList<String>()

      for (resolveInfo in packages) {
        try {
          if (resolveInfo.activityInfo != null) {
            val res =
                packageManager.getResourcesForApplication(resolveInfo.activityInfo.applicationInfo)

            val appPackage = resolveInfo.activityInfo.packageName
            val appName =
                if (resolveInfo.activityInfo.labelRes != 0) {
                  res.getString(resolveInfo.activityInfo.labelRes)
                } else {
                  resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager).toString()
                }

            // Check if the package name already exists
            if (!app_package_list.contains(appPackage)) {
              app_name_list.add(appName)
              app_package_list.add(appPackage)
            }
          }
        } catch (e: Exception) {
          p = e.toString()
        }
      }

      // Search for the app name in the list
      val index = app_package_list.indexOf(packageName)
      if (index >= 0 && index < app_name_list.size) {
        return app_name_list[index]
      }
    } catch (e: PackageManager.NameNotFoundException) {
      p = e.toString()
    }
    return p
  }

  @ReactMethod
  fun checkForPermission(promise: Promise) {
    val appOps: AppOpsManager =
        reactContext?.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode: Int =
        appOps.checkOpNoThrow(
            OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            reactContext?.getPackageName()!!
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
    val networkStatsByApp: NetworkStats
    var currentDataUsage = 0.0
    try {
      networkStatsByApp =
          networkStatsManager?.querySummary(networkType, subscriberId, startTime, endTime)!!
      do {
        val bucket: NetworkStats.Bucket = NetworkStats.Bucket()
        networkStatsByApp.getNextBucket(bucket)
        if (bucket.getUid() === packageUid) {
          currentDataUsage += bucket.rxBytes + bucket.txBytes
        }
      } while (networkStatsByApp.hasNextBucket())
    } catch (e: RemoteException) {
      e.printStackTrace()
    }
    return currentDataUsage
  }

  @ReactMethod
  fun getAppDataUsage(
      packageName: String,
      networkType: Int,
      startTime: Double,
      endTime: Double,
      promise: Promise
  ) {
    // get sim card
    val uid = getAppUid(packageName)
    if (networkType == ConnectivityManager.TYPE_MOBILE) {
      promise.resolve(
          getDataUsage(
              ConnectivityManager.TYPE_MOBILE,
              null,
              uid,
              startTime.toLong(),
              endTime.toLong()
          )
      )
    } else if (networkType == ConnectivityManager.TYPE_WIFI) {
      promise.resolve(
          getDataUsage(ConnectivityManager.TYPE_WIFI, "", uid, startTime.toLong(), endTime.toLong())
      )
    } else {
      promise.resolve(
          getDataUsage(
              ConnectivityManager.TYPE_MOBILE,
              "",
              uid,
              startTime.toLong(),
              endTime.toLong()
          ) +
              getDataUsage(
                  ConnectivityManager.TYPE_WIFI,
                  "",
                  uid,
                  startTime.toLong(),
                  endTime.toLong()
              )
      )
    }
  }

  private fun getAppUid(packageName: String): Int {
    // get app uid
    val packageManager: PackageManager? = reactContext?.getPackageManager()
    var info: ApplicationInfo? = null
    try {
      info = packageManager?.getApplicationInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }
    var uid = 0
    if (info != null) {
      uid = info.uid
    }
    return uid
  }

  companion object {
    const val NAME = "JustDiceReactNativeUsageStats"
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
