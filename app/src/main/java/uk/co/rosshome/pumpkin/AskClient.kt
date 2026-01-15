package uk.co.rosshome.pumpkin

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AskClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun ask(
        text: String,
        settings: SettingsState,
        deviceId: String,
        location: LocationPayload?,
    ): Result<AskResponse> {
        val payload = AskRequest(
            text = text,
            source = "android",
            device = deviceId,
            ts = Instant.now().toString(),
            ha_user_id = settings.haUserId.ifBlank { null },
            ha_user_name = settings.haUserName.ifBlank { null },
            location = location,
        )
        val body = json.encodeToString(payload)
        val request = Request.Builder()
            .url(settings.serverUrl + "/ask")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
                }
                if (settings.openAiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-OpenAI-Key", settings.openAiKey)
                }
            }
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("${response.code} ${response.message}")
                        )
                    }
                    Result.success(json.decodeFromString(responseBody))
                }
            } catch (exc: Exception) {
                Result.failure(exc)
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
