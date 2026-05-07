package com.gleanread.android.platform.page_context

import android.content.ComponentName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageContextAccessibilityStateTest {
    private val serviceName = ComponentName(
        "com.gleanread.android",
        "com.gleanread.android.platform.page_context.PageContextAccessibilityService",
    )

    @Test
    fun `enabled service list matches full component id`() {
        val enabledServices = serviceName.flattenToString()

        assertTrue(
            PageContextAccessibilityState.containsServiceId(
                rawServiceIds = enabledServices,
                componentName = serviceName,
            ),
        )
    }

    @Test
    fun `enabled service list matches short component id`() {
        val enabledServices = "com.gleanread.android/.platform.page_context.PageContextAccessibilityService"

        assertTrue(
            PageContextAccessibilityState.containsServiceId(
                rawServiceIds = enabledServices,
                componentName = serviceName,
            ),
        )
    }

    @Test
    fun `enabled service list ignores unrelated services`() {
        val enabledServices = listOf(
            "com.example/.OtherService",
            "com.gleanread.android/.OtherAccessibilityService",
        ).joinToString(":")

        assertFalse(
            PageContextAccessibilityState.containsServiceId(
                rawServiceIds = enabledServices,
                componentName = serviceName,
            ),
        )
    }
}
