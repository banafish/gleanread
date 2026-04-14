package com.gleanread.android.capture

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

object PageContextAccessibilityState {
    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val componentName = ComponentName(context, PageContextAccessibilityService::class.java)
        val serviceId = componentName.flattenToString()
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any { it.id == serviceId }
    }
}
