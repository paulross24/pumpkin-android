package uk.co.rosshome.pumpkin

import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Locale
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
    var lastHumanResponse by mutableStateOf<String?>(null)
        private set
    var isSending by mutableStateOf(false)
        private set

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val voiceNames = mutableStateListOf<String>()

    val availableVoices: List<String>
        get() = voiceNames

    private val deviceId: String =
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    init {
        tts = TextToSpeech(application) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.UK
                val voices = tts?.voices?.map { it.name }?.sorted() ?: emptyList()
                voiceNames.clear()
                voiceNames.addAll(voices)
            }
        }
    }

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
                lastHumanResponse = humanizeResponse(entry.responseBody)
            } else {
                lastError = entry.message
                lastHumanResponse = "Hmm, I couldn't reach Pumpkin. ${entry.message}"
            }
            if (settings.speakResponses) {
                val spoken = when {
                    entry.success && !lastHumanResponse.isNullOrBlank() -> lastHumanResponse
                    entry.success -> "All set. Sent to Pumpkin."
                    else -> "Error. ${entry.message}"
                }
                speak(spoken)
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

    private fun speak(text: String?) {
        if (!ttsReady || text.isNullOrBlank()) {
            return
        }
        val settings = settingsRepository.readSettings()
        if (settings.ttsVoiceName.isNotBlank()) {
            val voice = tts?.voices?.firstOrNull { it.name == settings.ttsVoiceName }
            if (voice != null) {
                tts?.voice = voice
            }
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pumpkin_response")
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onCleared()
    }

    private fun humanizeResponse(raw: String?): String {
        if (raw.isNullOrBlank()) {
            return "All set. I've sent that to Pumpkin."
        }
        val normalized = raw.trim()
        val parsed = try {
            Json.parseToJsonElement(normalized)
        } catch (exc: Exception) {
            null
        }
        if (parsed != null) {
            val status = parsed.jsonObject["status"]?.toString()?.trim('"')
            if (status == "ok") {
                return "Got it. Pumpkin has your update."
            }
        }
        val trimmed = if (normalized.length > 160) {
            normalized.take(160) + "..."
        } else {
            normalized
        }
        return "Pumpkin replied: $trimmed"
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
