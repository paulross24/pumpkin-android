package uk.co.rosshome.pumpkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AssistantBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val settings = SettingsRepository(context).readSettings()
        if (settings.assistantEnabled && settings.assistantStartOnBoot) {
            AssistantServiceController.start(context)
        }
    }
}
