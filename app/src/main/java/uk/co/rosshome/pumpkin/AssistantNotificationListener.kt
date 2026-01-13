package uk.co.rosshome.pumpkin

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AssistantNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reporter by lazy { AssistantEventReporter(applicationContext) }
    private var lastKey: String? = null
    private var lastSentAt: Long = 0

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) {
            return
        }
        val settings = SettingsRepository(applicationContext).readSettings()
        if (!settings.assistantEnabled || !settings.assistantIncludeNotifications) {
            return
        }
        val now = System.currentTimeMillis()
        val key = sbn.key
        if (key == lastKey && now - lastSentAt < 2000) {
            return
        }
        lastKey = key
        lastSentAt = now
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val detail = listOf(
            "pkg=${sbn.packageName}",
            title.takeIf { it.isNotBlank() }?.let { "title=$it" },
            text.takeIf { it.isNotBlank() }?.let { "text=$it" },
        ).filterNotNull().joinToString(" ")

        scope.launch {
            reporter.reportEvent("notification", detail)
        }
    }
}
