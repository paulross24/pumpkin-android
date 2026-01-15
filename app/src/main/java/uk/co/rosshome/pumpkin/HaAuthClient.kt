package uk.co.rosshome.pumpkin

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class HaTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("token_type") val tokenType: String = "",
)

@Serializable
data class HaUserResponse(
    val id: String = "",
    val name: String = "",
)

class HaAuthClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchangeCode(
        baseUrl: String,
        clientId: String,
        redirectUri: String,
        code: String,
        codeVerifier: String,
    ): Result<HaTokenResponse> {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("code_verifier", codeVerifier)
            .build()
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/auth/token")
            .post(body)
            .build()
        return executeTokenRequest(request)
    }

    suspend fun refreshToken(
        baseUrl: String,
        clientId: String,
        refreshToken: String,
    ): Result<HaTokenResponse> {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", clientId)
            .add("refresh_token", refreshToken)
            .build()
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/auth/token")
            .post(body)
            .build()
        return executeTokenRequest(request)
    }

    suspend fun fetchCurrentUser(baseUrl: String, accessToken: String): Result<HaUserResponse> {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/auth/current_user")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return withContext(Dispatchers.IO) {
            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HA user fetch failed: ${response.code}")
                    }
                    json.decodeFromString(HaUserResponse.serializer(), body)
                }
            }
        }
    }

    fun expiryEpochSeconds(expiresIn: Long): Long {
        return Instant.now().epochSecond + expiresIn
    }

    private suspend fun executeTokenRequest(request: Request): Result<HaTokenResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HA token request failed: ${response.code}")
                    }
                    json.decodeFromString(HaTokenResponse.serializer(), body)
                }
            }
        }
    }
}
