package uk.co.rosshome.pumpkin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val summaryClient: SummaryClient,
    private val errorsClient: ErrorsClient,
) : ViewModel() {
    var summary by mutableStateOf<SummaryResponse?>(null)
        private set
    var errors by mutableStateOf<List<ErrorReport>>(emptyList())
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            lastError = null
            try {
                val settings = settingsRepository.readSettings()
                val summaryResult = summaryClient.fetchSummary(settings)
                summaryResult.fold(
                    onSuccess = { summary = it },
                    onFailure = { exc -> lastError = exc.message ?: "summary failed" },
                )
                val errorsResult = errorsClient.fetchErrors(settings, limit = 10)
                errorsResult.fold(
                    onSuccess = { errors = it.errors },
                    onFailure = { exc -> lastError = exc.message ?: "errors failed" },
                )
            } finally {
                isLoading = false
            }
        }
    }
}

class HomeViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val summaryClient: SummaryClient,
    private val errorsClient: ErrorsClient,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(settingsRepository, summaryClient, errorsClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
