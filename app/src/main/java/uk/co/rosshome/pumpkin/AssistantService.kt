package uk.co.rosshome.pumpkin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AssistantService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var reporter: AssistantEventReporter
    private lateinit var triggerReceiver: AssistantTriggerReceiver
    private lateinit var carTelemetryManager: CarTelemetryManager
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        reporter = AssistantEventReporter(this)
        triggerReceiver = AssistantTriggerReceiver(reporter)
        carTelemetryManager = CarTelemetryManager(this)
        createNotificationChannel()
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

    companion object {
        const val ACTION_START = "uk.co.rosshome.pumpkin.ASSISTANT_START"
        const val ACTION_STOP = "uk.co.rosshome.pumpkin.ASSISTANT_STOP"
        private const val CHANNEL_ID = "pumpkin_assistant"
        private const val NOTIFICATION_ID = 2001
    }
}
