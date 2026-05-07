package com.gleanread.android.platform.page_context

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object PageContextAccessibilityState {
    fun isEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, PageContextAccessibilityService::class.java)
        return isEnabledByAccessibilityManager(context, componentName) ||
            isEnabledBySecureSettings(context, componentName)
    }

    private fun isEnabledByAccessibilityManager(
        context: Context,
        componentName: ComponentName,
    ): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any { serviceInfo ->
            matchesServiceId(serviceInfo.id, componentName)
        }
    }

    private fun isEnabledBySecureSettings(
        context: Context,
        componentName: ComponentName,
    ): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return containsServiceId(enabledServices, componentName)
    }

    internal fun containsServiceId(
        rawServiceIds: String?,
        componentName: ComponentName,
    ): Boolean {
        return rawServiceIds
            ?.split(ENABLED_SERVICE_SEPARATOR)
            ?.any { rawServiceId -> matchesServiceId(rawServiceId, componentName) }
            ?: false
    }

    private fun matchesServiceId(
        rawServiceId: String?,
        componentName: ComponentName,
    ): Boolean {
        val serviceId = rawServiceId?.trim().orEmpty()
        if (serviceId.isBlank()) return false
        val parsedComponent = ComponentName.unflattenFromString(serviceId)
        return parsedComponent == componentName ||
            serviceId == componentName.flattenToString() ||
            serviceId == componentName.flattenToShortString()
    }

    private const val ENABLED_SERVICE_SEPARATOR = ':'
}
