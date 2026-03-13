package io.justdice.usagestats

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule

/**
 * New Architecture (TurboModules) implementation.
 * Extends the codegen-generated NativeUsageStatsSpec and delegates to the shared Impl class.
 */
@ReactModule(name = UsageStatsManagerModuleImpl.MODULE_NAME)
class UsageStatsManagerModule(reactContext: ReactApplicationContext) :
    NativeUsageStatsSpec(reactContext) {

  private val impl = UsageStatsManagerModuleImpl(reactContext)

  override fun getName(): String = UsageStatsManagerModuleImpl.MODULE_NAME

  override fun getConstants(): Map<String, Any>? = impl.getConstants()

  override fun queryUsageStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    impl.queryUsageStats(interval, startTime, endTime, promise)
  }

  override fun queryAndAggregateUsageStats(startTime: Double, endTime: Double, promise: Promise) {
    impl.queryAndAggregateUsageStats(startTime, endTime, promise)
  }

  override fun queryEvents(startTime: Double, endTime: Double, promise: Promise) {
    impl.queryEvents(startTime, endTime, promise)
  }

  override fun queryEventsStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    impl.queryEventsStats(interval, startTime, endTime, promise)
  }

  override fun showUsageAccessSettings(packageName: String) {
    impl.showUsageAccessSettings(packageName)
  }

  override fun checkForPermission(promise: Promise) {
    impl.checkForPermission(promise)
  }

  override fun getAppDataUsage(
      packageName: String,
      networkType: Double,
      startTime: Double,
      endTime: Double,
      promise: Promise
  ) {
    impl.getAppDataUsage(packageName, networkType, startTime, endTime, promise)
  }

  companion object {
    const val NAME = UsageStatsManagerModuleImpl.MODULE_NAME
  }
}
