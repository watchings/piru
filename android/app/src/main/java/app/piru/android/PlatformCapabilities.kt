package app.piru.android

import android.os.Build

object PlatformCapabilities {
    val healthConnectAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= 26

    val cameraBarcodeAvailable: Boolean
        get() = true

    val pdfExportAvailable: Boolean
        get() = true

    val notificationsRequireRuntimePermission: Boolean
        get() = Build.VERSION.SDK_INT >= 33
}
