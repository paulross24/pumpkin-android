package uk.co.rosshome.pumpkin

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AssistantAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reporter by lazy { AssistantEventReporter(applicationContext) }
    private var lastSentAt: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        val settings = SettingsRepository(applicationContext).readSettings()
        if (!settings.assistantEnabled || !settings.assistantAccessibilityEnabled) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastSentAt < 1500) {
            return
        }
        lastSentAt = now
        val type = AccessibilityEvent.eventTypeToString(event.eventType)
        val pkg = event.packageName?.toString() ?: "unknown"
        val detail = "type=$type pkg=$pkg"
        scope.launch {
            reporter.reportEvent("accessibility", detail)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
