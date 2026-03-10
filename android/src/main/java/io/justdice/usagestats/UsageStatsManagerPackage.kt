package io.justdice.usagestats

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class UsageStatsManagerPackage : TurboReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == UsageStatsManagerModuleImpl.MODULE_NAME) {
      UsageStatsManagerModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      mapOf(
          UsageStatsManagerModuleImpl.MODULE_NAME to
              ReactModuleInfo(
                  UsageStatsManagerModuleImpl.MODULE_NAME,
                  UsageStatsManagerModuleImpl.MODULE_NAME,
                  false, // canOverrideExistingModule
                  false, // needsEagerInit
                  true,  // hasConstants
                  false, // isCxxModule
                  BuildConfig.IS_NEW_ARCHITECTURE_ENABLED // isTurboModule
              )
      )
    }
  }
}
