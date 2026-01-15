package uk.co.rosshome.pumpkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<SettingsState> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsState(
            serverUrl = SettingsRepository.DEFAULT_SERVER_URL,
            apiKey = "",
            openAiKey = "",
            profileName = "",
            includeLocation = false,
            speakResponses = false,
            ttsVoiceName = "",
            quietHours = "21:00-06:00",
            quietHoursDays = "weekdays",
            notificationStyle = "brief",
            assistantEnabled = false,
            assistantIncludeNotifications = true,
            assistantIncludeTriggers = true,
            assistantStartOnBoot = false,
            assistantAccessibilityEnabled = false,
            carTelemetryEnabled = false,
            carTelemetrySampleSeconds = 10,
            carTelemetrySyncMinutes = 30,
            alertPollMinutes = 60,
            carObdDeviceName = "",
            carObdDeviceAddress = "",
            carMake = "",
            carModel = "",
            carYear = "",
            carTrim = "",
        ),
    )

    fun updateServerUrl(value: String) {
        viewModelScope.launch {
            repository.updateServerUrl(value)
        }
    }

    fun updateApiKey(value: String) {
        viewModelScope.launch {
            repository.updateApiKey(value)
        }
    }

    fun updateOpenAiKey(value: String) {
        viewModelScope.launch {
            repository.updateOpenAiKey(value)
        }
    }

    fun updateProfileName(value: String) {
        viewModelScope.launch {
            repository.updateProfileName(value)
        }
    }

    fun updateIncludeLocation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateIncludeLocation(enabled)
        }
    }

    fun updateSpeakResponses(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSpeakResponses(enabled)
        }
    }

    fun updateTtsVoiceName(value: String) {
        viewModelScope.launch {
            repository.updateTtsVoiceName(value)
        }
    }

    fun updateQuietHours(value: String) {
        viewModelScope.launch {
            repository.updateQuietHours(value)
        }
    }

    fun updateQuietHoursDays(value: String) {
        viewModelScope.launch {
            repository.updateQuietHoursDays(value)
        }
    }

    fun updateNotificationStyle(value: String) {
        viewModelScope.launch {
            repository.updateNotificationStyle(value)
        }
    }

    fun updateAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAssistantEnabled(enabled)
        }
    }

    fun updateAssistantIncludeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAssistantIncludeNotifications(enabled)
        }
    }

    fun updateAssistantIncludeTriggers(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAssistantIncludeTriggers(enabled)
        }
    }

    fun updateAssistantStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAssistantStartOnBoot(enabled)
        }
    }

    fun updateAssistantAccessibilityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAssistantAccessibilityEnabled(enabled)
        }
    }

    fun updateCarTelemetryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCarTelemetryEnabled(enabled)
        }
    }

    fun updateCarTelemetrySampleSeconds(value: Int) {
        viewModelScope.launch {
            repository.updateCarTelemetrySampleSeconds(value)
        }
    }

    fun updateCarTelemetrySyncMinutes(value: Int) {
        viewModelScope.launch {
            repository.updateCarTelemetrySyncMinutes(value)
        }
    }

    fun updateAlertPollMinutes(value: Int) {
        viewModelScope.launch {
            repository.updateAlertPollMinutes(value)
        }
    }

    fun updateCarObdDeviceName(value: String) {
        viewModelScope.launch {
            repository.updateCarObdDeviceName(value)
        }
    }

    fun updateCarObdDeviceAddress(value: String) {
        viewModelScope.launch {
            repository.updateCarObdDeviceAddress(value)
        }
    }

    fun updateCarMake(value: String) {
        viewModelScope.launch {
            repository.updateCarMake(value)
        }
    }

    fun updateCarModel(value: String) {
        viewModelScope.launch {
            repository.updateCarModel(value)
        }
    }

    fun updateCarYear(value: String) {
        viewModelScope.launch {
            repository.updateCarYear(value)
        }
    }

    fun updateCarTrim(value: String) {
        viewModelScope.launch {
            repository.updateCarTrim(value)
        }
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
