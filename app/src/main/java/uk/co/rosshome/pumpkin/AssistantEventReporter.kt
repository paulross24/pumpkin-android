package uk.co.rosshome.pumpkin

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssistantEventReporter(
    private val context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
    private val ingestClient: IngestClient = IngestClient(),
    private val locationProvider: LocationProvider = LocationProvider(context),
) {
    private val deviceId: String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    suspend fun reportEvent(
        eventType: String,
        detail: String? = null,
    ) {
        val settings = settingsRepository.readSettings()
        if (!settings.assistantEnabled) {
            return
        }
        val location = if (settings.includeLocation && hasLocationPermission()) {
            val last = locationProvider.lastKnownLocation()
            if (last != null) {
                LocationPayload(
                    lat = last.latitude,
                    lon = last.longitude,
                    accuracy = last.accuracy.toDouble(),
                )
            } else {
                null
            }
        } else {
            null
        }
        val text = buildText(eventType, detail)
        withContext(Dispatchers.IO) {
            ingestClient.sendIngest(
                text = text,
                settings = settings,
                deviceId = deviceId,
                location = location,
                source = "android-assistant",
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildText(eventType: String, detail: String?): String {
        val suffix = detail?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
        return "assistant_event:$eventType$suffix"
    }
}
