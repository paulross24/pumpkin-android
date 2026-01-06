package uk.co.rosshome.pumpkin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class SummaryClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchSummary(settings: SettingsState): Result<SummaryResponse> {
        val request = Request.Builder()
            .url(settings.serverUrl + "/summary?status=pending&limit=5")
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
}
