package uk.co.rosshome.pumpkin

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object HaAuthFlow {
    const val REDIRECT_URI = "https://pumpkin.rosshome.co.uk/ha/callback"

    fun startLogin(context: Context, settings: SettingsState, repository: SettingsRepository) {
        repository.clearHaAuthPending()
        val verifier = generateVerifier()
        val state = generateState()
        repository.setHaAuthPending(state, verifier)
        val authUri = buildAuthorizeUri(
            baseUrl = settings.haBaseUrl,
            clientId = settings.haClientId,
            redirectUri = REDIRECT_URI,
            state = state,
            codeChallenge = codeChallenge(verifier),
        )
        CustomTabsIntent.Builder().build().launchUrl(context, authUri)
    }

    fun buildAuthorizeUri(
        baseUrl: String,
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
    ): Uri {
        return Uri.parse(baseUrl.trimEnd('/') + "/auth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    private fun generateVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
