package uk.co.rosshome.pumpkin

import android.content.Context
import android.content.Intent

object AmazonMusicLauncher {
    private val packages = listOf("com.amazon.mp3", "com.amazon.music")

    fun open(context: Context): Boolean {
        val appContext = context.applicationContext
        val intent = packages.asSequence()
            .mapNotNull { pkg -> appContext.packageManager.getLaunchIntentForPackage(pkg) }
            .firstOrNull()
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return if (intent != null) {
            appContext.startActivity(intent)
            true
        } else {
            false
        }
    }
}
