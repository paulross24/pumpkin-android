package uk.co.rosshome.pumpkin

import android.content.Context
import android.provider.Settings

object AssistantStatus {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        return runCatching {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return@runCatching false
            enabled.split(":").any { entry ->
                entry.contains(context.packageName)
            }
        }.getOrDefault(false)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return runCatching {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_accessibility_services",
            ) ?: return@runCatching false
            enabled.split(":").any { entry ->
                entry.contains(context.packageName)
            }
        }.getOrDefault(false)
    }
}
