package uk.co.rosshome.pumpkin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class LlmConfigClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun pushConfig(settings: SettingsState): Result<Unit> {
        val payload = mapOf(
            "api_key" to settings.openAiKey,
            "model" to "gpt-4o-mini",
        )
        val body = json.encodeToString(payload)
        val request = Request.Builder()
            .url(settings.serverUrl + "/llm/config")
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
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("${response.code} ${response.message}")
                        )
                    }
                    Result.success(Unit)
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
