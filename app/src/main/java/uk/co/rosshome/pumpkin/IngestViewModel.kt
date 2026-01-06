package uk.co.rosshome.pumpkin

import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class IngestViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val ingestClient: IngestClient,
    private val locationProvider: LocationProvider,
) : AndroidViewModel(application) {
    val logs = mutableStateListOf<IngestLogEntry>()
    var lastResponse by mutableStateOf<String?>(null)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var isSending by mutableStateOf(false)
        private set

    private val deviceId: String =
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    fun sendText(text: String) {
        if (text.isBlank()) {
            return
        }
        viewModelScope.launch {
            isSending = true
            lastError = null
            val settings = settingsRepository.readSettings()
            val locationPayload = if (settings.includeLocation && hasLocationPermission()) {
                val location = locationProvider.lastKnownLocation()
                if (location != null) {
                    LocationPayload(
                        lat = location.latitude,
                        lon = location.longitude,
                        accuracy = location.accuracy.toDouble(),
                    )
                } else {
                    null
                }
            } else {
                null
            }
            val entry = ingestClient.sendIngest(text, settings, deviceId, locationPayload)
            logs.add(0, entry)
            if (entry.success) {
                lastResponse = entry.responseBody ?: "no response body"
            } else {
                lastError = entry.message
            }
            isSending = false
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

class IngestViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val ingestClient: IngestClient,
    private val locationProvider: LocationProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IngestViewModel(
                application,
                settingsRepository,
                ingestClient,
                locationProvider,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
