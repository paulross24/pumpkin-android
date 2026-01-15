package uk.co.rosshome.pumpkin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AssistantService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var reporter: AssistantEventReporter
    private lateinit var triggerReceiver: AssistantTriggerReceiver
    private lateinit var carTelemetryManager: CarTelemetryManager
    private lateinit var settingsRepository: SettingsRepository
    private val notificationsClient = NotificationsClient()
    private var alertJob: Job? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        reporter = AssistantEventReporter(this)
        triggerReceiver = AssistantTriggerReceiver(reporter)
        carTelemetryManager = CarTelemetryManager(this)
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
        createAlertNotificationChannel()
        registerTriggers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    isRunning = true
                    scope.launch { reporter.reportEvent("assistant_started") }
                }
                carTelemetryManager.start()
                startAlertPolling()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(triggerReceiver) }
        if (isRunning) {
            scope.launch { reporter.reportEvent("assistant_stopped") }
        }
        carTelemetryManager.stop()
        alertJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerTriggers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
        }
        registerReceiver(triggerReceiver, filter)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Pumpkin Assistant")
            .setContentText("Listening for triggers and notifications")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pumpkin Assistant",
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = "Foreground service for Pumpkin assistant"
        manager.createNotificationChannel(channel)
    }

    private fun createAlertNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Pumpkin Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = "Important alerts from Pumpkin"
        manager.createNotificationChannel(channel)
    }

    private fun startAlertPolling() {
        if (alertJob?.isActive == true) return
        alertJob = scope.launch {
            while (isActive) {
                val settings = settingsRepository.readSettings()
                if (!settings.assistantEnabled || !settings.assistantIncludeNotifications) {
                    delay(ALERT_POLL_INTERVAL_MS)
                    continue
                }
                val result = notificationsClient.fetchNotifications(settings, limit = 10)
                result.getOrNull()?.let { response ->
                    handleNotifications(settings, response.notifications)
                }
                delay(ALERT_POLL_INTERVAL_MS)
            }
        }
    }

    private fun handleNotifications(settings: SettingsState, notifications: List<NotificationItem>) {
        if (notifications.isEmpty()) return
        val prefs = getSharedPreferences(ALERT_PREFS, Context.MODE_PRIVATE)
        val lastId = prefs.getInt(ALERT_PREF_LAST_ID, 0)
        val newItems = notifications.filter { it.id > lastId }.sortedBy { it.id }
        if (newItems.isEmpty()) return
        newItems.forEach { item ->
            showAlertNotification(settings, item)
        }
        val maxId = newItems.maxOf { it.id }
        prefs.edit().putInt(ALERT_PREF_LAST_ID, maxId).apply()
    }

    private fun showAlertNotification(settings: SettingsState, item: NotificationItem) {
        val url = settings.serverUrl + "/ui/car/alerts"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pending = PendingIntent.getActivity(
            this,
            item.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = "Pumpkin Alert"
        val text = item.message ?: "Car telemetry alert"
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ALERT_NOTIFICATION_BASE_ID + item.id, notification)
    }

    companion object {
        const val ACTION_START = "uk.co.rosshome.pumpkin.ASSISTANT_START"
        const val ACTION_STOP = "uk.co.rosshome.pumpkin.ASSISTANT_STOP"
        private const val CHANNEL_ID = "pumpkin_assistant"
        private const val NOTIFICATION_ID = 2001
        private const val ALERT_CHANNEL_ID = "pumpkin_alerts"
        private const val ALERT_NOTIFICATION_BASE_ID = 40000
        private const val ALERT_PREFS = "pumpkin_alerts"
        private const val ALERT_PREF_LAST_ID = "last_alert_id"
        private const val ALERT_POLL_INTERVAL_MS = 60 * 60 * 1000L
    }
}
