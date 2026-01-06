package uk.co.rosshome.pumpkin

import android.content.Context
import android.os.Build
import java.time.Instant
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CrashReporter(context: Context) {
    private val settingsRepository = SettingsRepository(context)
    private val client = OkHttpClient()

    fun reportCrash(throwable: Throwable) {
        val settings = settingsRepository.readSettings()
        if (settings.serverUrl.isBlank()) {
            return
        }
        val payload = JSONObject()
        payload.put("message", throwable.message ?: "crash")
        payload.put("stack", throwable.stackTraceToString())
        payload.put("ts", Instant.now().toString())
        payload.put("device", Build.MODEL)
        payload.put("manufacturer", Build.MANUFACTURER)
        payload.put("sdk", Build.VERSION.SDK_INT)
        payload.put("app", "Pumpkin Android")

        val request = Request.Builder()
            .url(settings.serverUrl + "/errors")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    addHeader("X-Pumpkin-Key", settings.apiKey)
                }
            }
            .build()

        try {
            client.newCall(request).execute().close()
        } catch (exc: Exception) {
            return
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
