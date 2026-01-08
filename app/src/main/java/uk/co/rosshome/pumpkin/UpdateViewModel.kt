package uk.co.rosshome.pumpkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val updateClient: UpdateClient,
) : ViewModel() {
    var latest: ReleaseInfo? = null
        private set
    var lastError: String? = null
        private set
    var isChecking: Boolean = false
        private set

    fun check() {
        viewModelScope.launch {
            isChecking = true
            lastError = null
            try {
                val result = updateClient.fetchLatest()
                result.fold(
                    onSuccess = { latest = it },
                    onFailure = { exc -> lastError = exc.message ?: "update check failed" },
                )
            } finally {
                isChecking = false
            }
        }
    }
}

class UpdateViewModelFactory(
    private val updateClient: UpdateClient,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UpdateViewModel(updateClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
