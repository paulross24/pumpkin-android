package uk.co.rosshome.pumpkin

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class IngestClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun sendIngest(
        text: String,
        settings: SettingsState,
        deviceId: String,
        location: LocationPayload?,
    ): IngestLogEntry {
        val payload = IngestRequest(
            text = text,
            source = "android",
            device = deviceId,
            ts = Instant.now().toString(),
            location = location,
        )
        val body = json.encodeToString(payload)
        val request = Request.Builder()
            .url(settings.serverUrl + "/ingest")
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
                    val responseBody = response.body?.string()
                    val message = if (response.isSuccessful) {
                        "${response.code} ${response.message}"
                    } else {
                        "${response.code} ${response.message}"
                    }
                    IngestLogEntry(
                        timestamp = Instant.now().toString(),
                        success = response.isSuccessful,
                        message = message,
                        responseBody = responseBody,
                    )
                }
            } catch (exc: Exception) {
                IngestLogEntry(
                    timestamp = Instant.now().toString(),
                    success = false,
                    message = exc.message ?: "request failed",
                    responseBody = null,
                )
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
