package uk.co.rosshome.pumpkin

import java.time.Instant
import java.util.UUID
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
        source: String = "android",
    ): IngestLogEntry {
        val requestId = UUID.randomUUID().toString()
        val payload = IngestRequest(
            request_id = requestId,
            text = text,
            source = source,
            device = deviceId,
            ts = Instant.now().toString(),
            ha_user_id = settings.haUserId.ifBlank { null },
            ha_user_name = settings.haUserName.ifBlank { null },
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
            }
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    val message = if (response.isSuccessful) {
                        "${response.code} ${response.message} (${requestId})"
                    } else {
                        "${response.code} ${response.message} (${requestId})"
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
