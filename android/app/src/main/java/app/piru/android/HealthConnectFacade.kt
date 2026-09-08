package app.piru.android

import android.content.Context
import android.os.Build

/**
 * Optional adapter boundary. Core journal behavior never depends on Health Connect
 * or Google Play services; installations without it must remain fully functional.
 */
interface HealthConnectFacade {
    suspend fun hasPermissions(): Boolean
    suspend fun requestPermissions(): Boolean
    suspend fun deletePiruData(): Boolean
}

object HealthConnectProvider {
    fun get(context: Context): HealthConnectFacade? {
        if (Build.VERSION.SDK_INT < 26) return null
        return UnavailableHealthConnect
    }

    private object UnavailableHealthConnect : HealthConnectFacade {
        override suspend fun hasPermissions() = false
        override suspend fun requestPermissions() = false
        override suspend fun deletePiruData() = false
    }
}
