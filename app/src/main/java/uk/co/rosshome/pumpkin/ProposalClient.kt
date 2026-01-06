package uk.co.rosshome.pumpkin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class ProposalClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchProposals(settings: SettingsState, status: String?, limit: Int): Result<ProposalsResponse> {
        val url = buildString {
            append(settings.serverUrl)
            append("/proposals")
            val params = mutableListOf<String>()
            if (!status.isNullOrBlank()) {
                params.add("status=$status")
            }
            if (limit > 0) {
                params.add("limit=$limit")
            }
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
        val request = Request.Builder()
            .url(url)
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
