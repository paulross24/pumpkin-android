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
            includeLocation = false,
            speakResponses = false,
            ttsVoiceName = "",
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
