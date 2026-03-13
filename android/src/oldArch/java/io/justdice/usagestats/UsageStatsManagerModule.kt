package io.justdice.usagestats

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule

/**
 * Old Architecture implementation.
 * Extends ReactContextBaseJavaModule and delegates to the shared Impl class.
 */
@ReactModule(name = UsageStatsManagerModuleImpl.MODULE_NAME)
class UsageStatsManagerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

  private val impl = UsageStatsManagerModuleImpl(reactContext)

  override fun getName(): String = UsageStatsManagerModuleImpl.MODULE_NAME

  override fun getConstants(): Map<String, Any>? = impl.getConstants()

  @ReactMethod
  fun queryUsageStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    impl.queryUsageStats(interval, startTime, endTime, promise)
  }

  @ReactMethod
  fun queryAndAggregateUsageStats(startTime: Double, endTime: Double, promise: Promise) {
    impl.queryAndAggregateUsageStats(startTime, endTime, promise)
  }

  @ReactMethod
  fun queryEvents(startTime: Double, endTime: Double, promise: Promise) {
    impl.queryEvents(startTime, endTime, promise)
  }

  @ReactMethod
  fun queryEventsStats(interval: Double, startTime: Double, endTime: Double, promise: Promise) {
    impl.queryEventsStats(interval, startTime, endTime, promise)
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  fun showUsageAccessSettings(packageName: String) {
    impl.showUsageAccessSettings(packageName)
  }

  @ReactMethod
  fun checkForPermission(promise: Promise) {
    impl.checkForPermission(promise)
  }

  @ReactMethod
  fun getAppDataUsage(
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
