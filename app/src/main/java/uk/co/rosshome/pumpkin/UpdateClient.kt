package uk.co.rosshome.pumpkin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
private data class ReleaseAsset(
    val name: String = "",
    val browser_download_url: String = "",
)

@Serializable
private data class ReleaseResponse(
    val tag_name: String = "",
    val html_url: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
)

class UpdateClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatest(): Result<ReleaseInfo> {
        val request = Request.Builder()
            .url("https://api.github.com/repos/paulross24/pumpkin-android/releases/latest")
            .get()
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
                    val decoded = json.decodeFromString<ReleaseResponse>(body)
                    val asset = decoded.assets.firstOrNull { it.name.endsWith(".apk") }
                    val info = ReleaseInfo(
                        tag = decoded.tag_name,
                        htmlUrl = decoded.html_url,
                        apkUrl = asset?.browser_download_url,
                    )
                    Result.success(info)
                }
            } catch (exc: Exception) {
                Result.failure(exc)
            }
        }
    }
}

@Serializable
data class ReleaseInfo(
    val tag: String,
    val htmlUrl: String,
    val apkUrl: String? = null,
)
