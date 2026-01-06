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
        val payload = mapOf(
            "text" to text,
            "source" to "android",
            "device" to deviceId,
            "ts" to Instant.now().toString(),
            "location" to location,
        )
        val body = json.encodeToString(payload)
        val request = Request.Builder()
            .url(settings.serverUrl + "/ask")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
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
