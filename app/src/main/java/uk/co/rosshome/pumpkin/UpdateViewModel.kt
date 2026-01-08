package uk.co.rosshome.pumpkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.co.rosshome.pumpkin.BuildConfig

class UpdateViewModel(
    private val updateClient: UpdateClient,
) : ViewModel() {
    var latest: ReleaseInfo? = null
        private set
    var updateAvailable: Boolean = false
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
                    onSuccess = {
                        latest = it
                        updateAvailable = isUpdateAvailable(BuildConfig.VERSION_NAME, it.tag)
                    },
                    onFailure = { exc -> lastError = exc.message ?: "update check failed" },
                )
            } finally {
                isChecking = false
            }
        }
    }
}

private fun isUpdateAvailable(current: String, tag: String): Boolean {
    val currentParts = normalizeVersion(current)
    val tagParts = normalizeVersion(tag)
    val max = maxOf(currentParts.size, tagParts.size)
    for (idx in 0 until max) {
        val left = currentParts.getOrElse(idx) { 0 }
        val right = tagParts.getOrElse(idx) { 0 }
        if (right > left) return true
        if (right < left) return false
    }
    return false
}

private fun normalizeVersion(value: String): List<Int> {
    val cleaned = value.trim().removePrefix("v").removePrefix("V")
    return cleaned.split(".", "-", "_")
        .mapNotNull { part -> part.toIntOrNull() }
        .ifEmpty { listOf(0) }
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
