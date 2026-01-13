package uk.co.rosshome.pumpkin

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AssistantTriggerReceiver(
    private val reporter: AssistantEventReporter,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository(context).readSettings()
        if (!settings.assistantEnabled || !settings.assistantIncludeTriggers) {
            return
        }
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val detail = when (action) {
                Intent.ACTION_SCREEN_ON -> "screen_on"
                Intent.ACTION_SCREEN_OFF -> "screen_off"
                Intent.ACTION_USER_PRESENT -> "user_present"
                Intent.ACTION_POWER_CONNECTED -> "power_connected"
                Intent.ACTION_POWER_DISCONNECTED -> "power_disconnected"
                Intent.ACTION_HEADSET_PLUG -> buildHeadsetDetail(intent)
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> buildBluetoothDetail(intent)
                else -> null
            }
            val eventType = when (action) {
                Intent.ACTION_HEADSET_PLUG -> "headset"
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> "bluetooth"
                Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> "power"
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT -> "screen"
                else -> "system"
            }
            if (detail != null) {
                reporter.reportEvent(eventType, detail)
            }
            pending.finish()
        }
    }

    private fun buildHeadsetDetail(intent: Intent): String {
        val state = intent.getIntExtra("state", -1)
        val name = intent.getStringExtra("name") ?: "unknown"
        val mic = intent.getIntExtra("microphone", -1)
        return "headset state=$state name=$name mic=$mic"
    }

    private fun buildBluetoothDetail(intent: Intent): String {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
        return "connection_state=$state"
    }
}
