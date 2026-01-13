package uk.co.rosshome.pumpkin

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object AssistantServiceController {
    fun start(context: Context) {
        runCatching {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = AssistantService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }.onFailure { exc ->
            CrashReporter(context).reportNonFatal(exc)
        }
    }

    fun stop(context: Context) {
        runCatching {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = AssistantService.ACTION_STOP
            }
            context.startService(intent)
        }.onFailure { exc ->
            CrashReporter(context).reportNonFatal(exc)
        }
    }
}
