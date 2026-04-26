package com.gleanread.android.data.sync

import android.content.Context
import java.util.UUID

fun interface DeviceIdProvider {
    fun currentDeviceId(): String
}

class DeviceIdentityStore(
    context: Context,
) : DeviceIdProvider {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun currentDeviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    private companion object {
        const val PREFERENCES_NAME = "glean_sync_identity"
        const val KEY_DEVICE_ID = "device_id"
    }
}

object LocalDeviceIdProvider : DeviceIdProvider {
    override fun currentDeviceId(): String = "local-device"
}
