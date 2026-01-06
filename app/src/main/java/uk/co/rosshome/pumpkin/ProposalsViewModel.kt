package uk.co.rosshome.pumpkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProposalsViewModel(
    private val settingsRepository: SettingsRepository,
    private val proposalClient: ProposalClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ProposalsState())
    val state: StateFlow<ProposalsState> = _state

    fun refresh(status: String? = "pending", limit: Int = 50) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val settings = settingsRepository.readSettings()
            val result = proposalClient.fetchProposals(settings, status, limit)
            _state.value = result.fold(
                onSuccess = { response ->
                    _state.value.copy(
                        isLoading = false,
                        proposals = response.proposals,
                        error = null,
                    )
                },
                onFailure = { exc ->
                    _state.value.copy(
                        isLoading = false,
                        error = exc.message ?: "failed to load",
                    )
                },
            )
        }
    }
}

data class ProposalsState(
    val isLoading: Boolean = false,
    val proposals: List<Proposal> = emptyList(),
    val error: String? = null,
)

class ProposalsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val proposalClient: ProposalClient,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProposalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProposalsViewModel(settingsRepository, proposalClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
