package uk.co.rosshome.pumpkin

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

class MediaControlHelper(private val context: Context) {
    private val preferredPackages = listOf("com.amazon.mp3", "com.amazon.music")

    fun playPause(): String = control { controller ->
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
            "Paused"
        } else {
            controller.transportControls.play()
            "Playing"
        }
    }

    fun next(): String = control { controller ->
        controller.transportControls.skipToNext()
        "Next track"
    }

    fun previous(): String = control { controller ->
        controller.transportControls.skipToPrevious()
        "Previous track"
    }

    private fun control(action: (MediaController) -> String): String {
        val controller = getController() ?: return "No active media session"
        return runCatching { action(controller) }.getOrElse { "Media control failed" }
    }

    private fun getController(): MediaController? {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, AssistantNotificationListener::class.java)
        val controllers = runCatching { manager.getActiveSessions(component) }.getOrDefault(emptyList())
        if (controllers.isEmpty()) {
            return null
        }
        return controllers.firstOrNull { it.packageName in preferredPackages } ?: controllers.first()
    }
}
