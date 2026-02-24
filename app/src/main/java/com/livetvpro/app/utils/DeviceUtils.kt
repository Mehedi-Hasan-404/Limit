package com.livetvpro.app.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build

object DeviceUtils {

    enum class DeviceType {
        PHONE,
        TABLET,
        TV,
        WATCH,
        AUTOMOTIVE,
        FOLDABLE,
        EMULATOR
    }

    var deviceType: DeviceType = DeviceType.PHONE
        private set

    val isTvDevice: Boolean get() = deviceType == DeviceType.TV
    val isTablet: Boolean get() = deviceType == DeviceType.TABLET
    val isPhone: Boolean get() = deviceType == DeviceType.PHONE
    val isWatch: Boolean get() = deviceType == DeviceType.WATCH
    val isAutomotive: Boolean get() = deviceType == DeviceType.AUTOMOTIVE
    val isFoldable: Boolean get() = deviceType == DeviceType.FOLDABLE
    val isEmulator: Boolean get() = deviceType == DeviceType.EMULATOR

    fun init(context: Context) {

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        deviceType = when (uiModeManager.currentModeType) {
            Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV
            Configuration.UI_MODE_TYPE_WATCH -> DeviceType.WATCH
            Configuration.UI_MODE_TYPE_CAR -> DeviceType.AUTOMOTIVE
            else -> detectHandheld(context)
        }

        if (deviceType != DeviceType.TV) {
            if (context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")) {
                deviceType = DeviceType.TV
            }
        }

        if (deviceType == DeviceType.TV) return

        if (deviceType == DeviceType.PHONE || deviceType == DeviceType.TABLET) {
            if (isX86OrEmulator()) {
                deviceType = DeviceType.EMULATOR
            }
        }
    }

    private fun isX86OrEmulator(): Boolean {

        val supportedAbis = Build.SUPPORTED_ABIS
        val isX86 = supportedAbis.any { it.startsWith("x86") }

        val isEmulatorBuild = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("Emulator", ignoreCase = true)
                || Build.MODEL.contains("Android SDK", ignoreCase = true)
                || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic"))

        return isX86 || isEmulatorBuild
    }

    private fun detectHandheld(context: Context): DeviceType {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (context.packageManager.hasSystemFeature("android.hardware.sensor.hinge_angle")) {
                return DeviceType.FOLDABLE
            }
        }

        val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
        if (smallestWidthDp >= 600) {
            return DeviceType.TABLET
        }

        return DeviceType.PHONE
    }
}
