package uk.co.rosshome.pumpkin

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadState())
    val settingsFlow: StateFlow<SettingsState> = _settings

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in trackedKeys) {
                _settings.value = loadState()
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun readSettings(): SettingsState = _settings.value

    fun updateServerUrl(value: String) {
        val normalized = normalizeServerUrl(value)
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        _settings.value = loadState()
    }

    fun updateApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateOpenAiKey(value: String) {
        prefs.edit().putString(KEY_OPENAI_KEY, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateIncludeLocation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_LOCATION, enabled).apply()
        _settings.value = loadState()
    }

    fun updateSpeakResponses(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPEAK_RESPONSES, enabled).apply()
        _settings.value = loadState()
    }

    fun updateTtsVoiceName(value: String) {
        prefs.edit().putString(KEY_TTS_VOICE, value).apply()
        _settings.value = loadState()
    }

    fun updateQuietHours(value: String) {
        prefs.edit().putString(KEY_QUIET_HOURS, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateQuietHoursDays(value: String) {
        prefs.edit().putString(KEY_QUIET_HOURS_DAYS, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateNotificationStyle(value: String) {
        prefs.edit().putString(KEY_NOTIFICATION_STYLE, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_ENABLED, enabled).apply()
        _settings.value = loadState()
    }

    fun updateAssistantIncludeNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_NOTIFICATIONS, enabled).apply()
        _settings.value = loadState()
    }

    fun updateAssistantIncludeTriggers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_TRIGGERS, enabled).apply()
        _settings.value = loadState()
    }

    fun updateAssistantStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_BOOT, enabled).apply()
        _settings.value = loadState()
    }

    fun updateAssistantAccessibilityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSISTANT_ACCESSIBILITY, enabled).apply()
        _settings.value = loadState()
    }

    fun updateCarTelemetryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAR_TELEMETRY_ENABLED, enabled).apply()
        _settings.value = loadState()
    }

    fun updateCarTelemetrySampleSeconds(value: Int) {
        prefs.edit().putInt(KEY_CAR_SAMPLE_SECONDS, value).apply()
        _settings.value = loadState()
    }

    fun updateCarTelemetrySyncMinutes(value: Int) {
        prefs.edit().putInt(KEY_CAR_SYNC_MINUTES, value).apply()
        _settings.value = loadState()
    }

    fun updateCarObdDeviceName(value: String) {
        prefs.edit().putString(KEY_CAR_OBD_NAME, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateCarObdDeviceAddress(value: String) {
        prefs.edit().putString(KEY_CAR_OBD_ADDRESS, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateCarMake(value: String) {
        prefs.edit().putString(KEY_CAR_MAKE, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateCarModel(value: String) {
        prefs.edit().putString(KEY_CAR_MODEL, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateCarYear(value: String) {
        prefs.edit().putString(KEY_CAR_YEAR, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateCarTrim(value: String) {
        prefs.edit().putString(KEY_CAR_TRIM, value.trim()).apply()
        _settings.value = loadState()
    }

    private fun loadState(): SettingsState {
        return SettingsState(
            serverUrl = normalizeServerUrl(
                prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
            ),
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            openAiKey = prefs.getString(KEY_OPENAI_KEY, "") ?: "",
            includeLocation = prefs.getBoolean(KEY_INCLUDE_LOCATION, false),
            speakResponses = prefs.getBoolean(KEY_SPEAK_RESPONSES, false),
            ttsVoiceName = prefs.getString(KEY_TTS_VOICE, "") ?: "",
            quietHours = prefs.getString(KEY_QUIET_HOURS, "21:00-06:00") ?: "21:00-06:00",
            quietHoursDays = prefs.getString(KEY_QUIET_HOURS_DAYS, "weekdays") ?: "weekdays",
            notificationStyle = prefs.getString(KEY_NOTIFICATION_STYLE, "brief") ?: "brief",
            assistantEnabled = prefs.getBoolean(KEY_ASSISTANT_ENABLED, false),
            assistantIncludeNotifications = prefs.getBoolean(KEY_ASSISTANT_NOTIFICATIONS, true),
            assistantIncludeTriggers = prefs.getBoolean(KEY_ASSISTANT_TRIGGERS, true),
            assistantStartOnBoot = prefs.getBoolean(KEY_ASSISTANT_BOOT, false),
            assistantAccessibilityEnabled = prefs.getBoolean(KEY_ASSISTANT_ACCESSIBILITY, false),
            carTelemetryEnabled = prefs.getBoolean(KEY_CAR_TELEMETRY_ENABLED, false),
            carTelemetrySampleSeconds = prefs.getInt(KEY_CAR_SAMPLE_SECONDS, 10),
            carTelemetrySyncMinutes = prefs.getInt(KEY_CAR_SYNC_MINUTES, 30),
            carObdDeviceName = prefs.getString(KEY_CAR_OBD_NAME, "") ?: "",
            carObdDeviceAddress = prefs.getString(KEY_CAR_OBD_ADDRESS, "") ?: "",
            carMake = prefs.getString(KEY_CAR_MAKE, "") ?: "",
            carModel = prefs.getString(KEY_CAR_MODEL, "") ?: "",
            carYear = prefs.getString(KEY_CAR_YEAR, "") ?: "",
            carTrim = prefs.getString(KEY_CAR_TRIM, "") ?: "",
        )
    }

    private fun normalizeServerUrl(value: String): String {
        return value.trim().removeSuffix("/")
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_OPENAI_KEY = "openai_key"
        private const val KEY_INCLUDE_LOCATION = "include_location"
        private const val KEY_SPEAK_RESPONSES = "speak_responses"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_QUIET_HOURS = "quiet_hours"
        private const val KEY_QUIET_HOURS_DAYS = "quiet_hours_days"
        private const val KEY_NOTIFICATION_STYLE = "notification_style"
        private const val KEY_ASSISTANT_ENABLED = "assistant_enabled"
        private const val KEY_ASSISTANT_NOTIFICATIONS = "assistant_notifications"
        private const val KEY_ASSISTANT_TRIGGERS = "assistant_triggers"
        private const val KEY_ASSISTANT_BOOT = "assistant_boot"
        private const val KEY_ASSISTANT_ACCESSIBILITY = "assistant_accessibility"
        private const val KEY_CAR_TELEMETRY_ENABLED = "car_telemetry_enabled"
        private const val KEY_CAR_SAMPLE_SECONDS = "car_sample_seconds"
        private const val KEY_CAR_SYNC_MINUTES = "car_sync_minutes"
        private const val KEY_CAR_OBD_NAME = "car_obd_name"
        private const val KEY_CAR_OBD_ADDRESS = "car_obd_address"
        private const val KEY_CAR_MAKE = "car_make"
        private const val KEY_CAR_MODEL = "car_model"
        private const val KEY_CAR_YEAR = "car_year"
        private const val KEY_CAR_TRIM = "car_trim"
        private val trackedKeys = setOf(
            KEY_SERVER_URL,
            KEY_API_KEY,
            KEY_OPENAI_KEY,
            KEY_INCLUDE_LOCATION,
            KEY_SPEAK_RESPONSES,
            KEY_TTS_VOICE,
            KEY_QUIET_HOURS,
            KEY_QUIET_HOURS_DAYS,
            KEY_NOTIFICATION_STYLE,
            KEY_ASSISTANT_ENABLED,
            KEY_ASSISTANT_NOTIFICATIONS,
            KEY_ASSISTANT_TRIGGERS,
            KEY_ASSISTANT_BOOT,
            KEY_ASSISTANT_ACCESSIBILITY,
            KEY_CAR_TELEMETRY_ENABLED,
            KEY_CAR_SAMPLE_SECONDS,
            KEY_CAR_SYNC_MINUTES,
            KEY_CAR_OBD_NAME,
            KEY_CAR_OBD_ADDRESS,
            KEY_CAR_MAKE,
            KEY_CAR_MODEL,
            KEY_CAR_YEAR,
            KEY_CAR_TRIM,
        )

        const val DEFAULT_SERVER_URL = "https://pumpkin.rosshome.co.uk"
    }
}
