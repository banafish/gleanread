package com.gleanread.android.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceIdentityStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("glean_sync_identity", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `currentDeviceId generates and reuses persisted id`() {
        val firstStore = DeviceIdentityStore(context)
        val firstId = firstStore.currentDeviceId()
        val secondStore = DeviceIdentityStore(context)
        val secondId = secondStore.currentDeviceId()

        assertTrue(firstId.isNotBlank())
        assertEquals(firstId, secondId)
    }
}
