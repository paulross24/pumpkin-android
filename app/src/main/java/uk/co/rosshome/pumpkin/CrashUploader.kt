package uk.co.rosshome.pumpkin

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CrashUploader(context: Context) {
    private val settingsRepository = SettingsRepository(context)
    private val store = CrashReportStore(context)
    private val client = OkHttpClient()

    fun uploadIfPresent(): String {
        val report = store.load() ?: return "no report"
        val settings = settingsRepository.readSettings()
        if (settings.serverUrl.isBlank()) {
            return "server url missing"
        }
        val request = Request.Builder()
            .url(settings.serverUrl + "/errors")
            .post(report.toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
                }
            }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    store.clear()
                    return "uploaded"
                }
                return "upload failed ${response.code}"
            }
        } catch (exc: Exception) {
            return "upload failed"
        }
    }

    fun hasReport(): Boolean {
        return store.load() != null
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
