package uk.co.rosshome.pumpkin

import android.content.Context
import android.provider.Settings

object AssistantStatus {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabled.split(":").any { entry ->
            entry.contains(context.packageName)
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_accessibility_services",
        ) ?: return false
        return enabled.split(":").any { entry ->
            entry.contains(context.packageName)
        }
    }
}
