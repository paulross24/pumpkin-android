package uk.co.rosshome.pumpkin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.encodeToString

class NotificationsClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchNotifications(settings: SettingsState, limit: Int = 10): Result<NotificationsResponse> {
        val request = Request.Builder()
            .url(settings.serverUrl + "/notifications?limit=$limit")
            .get()
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
                }
            }
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("${response.code} ${response.message}")
                        )
                    }
                    Result.success(json.decodeFromString(body))
                }
            } catch (exc: Exception) {
                Result.failure(exc)
            }
        }
    }

    suspend fun sendTestAlert(settings: SettingsState, message: String? = null): Result<String> {
        val payload = if (message.isNullOrBlank()) {
            "{}"
        } else {
            json.encodeToString(mapOf("message" to message))
        }
        val body = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(settings.serverUrl + "/notifications/test")
            .post(body)
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
                }
            }
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("${response.code} ${response.message}")
                        )
                    }
                    Result.success(bodyText)
                }
            } catch (exc: Exception) {
                Result.failure(exc)
            }
        }
    }
}
