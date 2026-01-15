package uk.co.rosshome.pumpkin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HaAuthCallbackActivity : ComponentActivity() {
    private val client = HaAuthClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val code = data?.getQueryParameter("code")
        val state = data?.getQueryParameter("state")
        val error = data?.getQueryParameter("error")
        val repository = SettingsRepository(applicationContext)

        if (!error.isNullOrBlank()) {
            repository.updateHaAuthError(error)
            finish()
            return
        }

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            repository.updateHaAuthError("Missing auth code")
            finish()
            return
        }

        val pending = repository.readHaAuthPending()
        if (pending == null || pending.first != state) {
            repository.updateHaAuthError("State mismatch")
            finish()
            return
        }

        val settings = repository.readSettings()
        val codeVerifier = pending.second

        lifecycleScope.launch {
            val tokenResult = client.exchangeCode(
                baseUrl = settings.haBaseUrl,
                clientId = settings.haClientId,
                redirectUri = HaAuthFlow.REDIRECT_URI,
                code = code,
                codeVerifier = codeVerifier,
            )
            tokenResult.fold(
                onSuccess = { token ->
                    val expiry = client.expiryEpochSeconds(token.expiresIn)
                    repository.updateHaTokens(token.accessToken, token.refreshToken, expiry)
                    repository.clearHaAuthPending()
                    repository.updateHaAuthError("")
                    client.fetchCurrentUser(settings.haBaseUrl, token.accessToken)
                        .onSuccess { user ->
                            if (user.name.isNotBlank()) {
                                repository.updateHaUser(user.name, user.id)
                                if (repository.readSettings().profileName.isBlank()) {
                                    repository.updateProfileName(user.name)
                                }
                            }
                        }
                },
                onFailure = { err ->
                    repository.updateHaAuthError(err.message ?: "Token exchange failed")
                },
            )
            finish()
        }
    }
}
