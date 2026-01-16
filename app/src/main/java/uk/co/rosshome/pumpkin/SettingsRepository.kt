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

    fun updateProfileName(value: String) {
        prefs.edit().putString(KEY_PROFILE_NAME, value.trim()).apply()
        _settings.value = loadState()
    }

    fun updateHaBaseUrl(value: String) {
        val normalized = normalizeServerUrl(value)
        prefs.edit().putString(KEY_HA_BASE_URL, normalized).apply()
        _settings.value = loadState()
    }

    fun updateHaClientId(value: String) {
        prefs.edit().putString(KEY_HA_CLIENT_ID, value.trim()).apply()
        _settings.value = loadState()
    }

    fun setHaAuthPending(state: String, verifier: String) {
        prefs.edit()
            .putString(KEY_HA_AUTH_STATE, state)
            .putString(KEY_HA_CODE_VERIFIER, verifier)
            .apply()
        _settings.value = loadState()
    }

    fun readHaAuthPending(): Pair<String, String>? {
        val state = prefs.getString(KEY_HA_AUTH_STATE, null)
        val verifier = prefs.getString(KEY_HA_CODE_VERIFIER, null)
        return if (!state.isNullOrBlank() && !verifier.isNullOrBlank()) {
            state to verifier
        } else {
            null
        }
    }

    fun clearHaAuthPending() {
        prefs.edit()
            .remove(KEY_HA_AUTH_STATE)
            .remove(KEY_HA_CODE_VERIFIER)
            .apply()
        _settings.value = loadState()
    }

    fun updateHaTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_HA_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_HA_REFRESH_TOKEN, refreshToken.trim())
            .putLong(KEY_HA_TOKEN_EXPIRY, expiresAt)
            .apply()
        _settings.value = loadState()
    }

    fun updateHaUser(name: String, userId: String) {
        prefs.edit()
            .putString(KEY_HA_USER_NAME, name.trim())
            .putString(KEY_HA_USER_ID, userId.trim())
            .apply()
        _settings.value = loadState()
    }

    fun updateHaAuthError(message: String) {
        prefs.edit().putString(KEY_HA_AUTH_ERROR, message.trim()).apply()
        _settings.value = loadState()
    }

    fun clearHaAuth() {
        prefs.edit()
            .remove(KEY_HA_ACCESS_TOKEN)
            .remove(KEY_HA_REFRESH_TOKEN)
            .remove(KEY_HA_TOKEN_EXPIRY)
            .remove(KEY_HA_USER_NAME)
            .remove(KEY_HA_USER_ID)
            .remove(KEY_HA_AUTH_ERROR)
            .remove(KEY_HA_AUTH_STATE)
            .remove(KEY_HA_CODE_VERIFIER)
            .apply()
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

    fun updateAlertPollMinutes(value: Int) {
        prefs.edit().putInt(KEY_ALERT_POLL_MINUTES, value).apply()
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
            profileName = prefs.getString(KEY_PROFILE_NAME, "") ?: "",
            haBaseUrl = normalizeServerUrl(
                prefs.getString(KEY_HA_BASE_URL, DEFAULT_HA_BASE_URL) ?: DEFAULT_HA_BASE_URL
            ),
            haClientId = prefs.getString(KEY_HA_CLIENT_ID, DEFAULT_HA_CLIENT_ID) ?: DEFAULT_HA_CLIENT_ID,
            haAccessToken = prefs.getString(KEY_HA_ACCESS_TOKEN, "") ?: "",
            haRefreshToken = prefs.getString(KEY_HA_REFRESH_TOKEN, "") ?: "",
            haTokenExpiry = prefs.getLong(KEY_HA_TOKEN_EXPIRY, 0L),
            haUserName = prefs.getString(KEY_HA_USER_NAME, "") ?: "",
            haUserId = prefs.getString(KEY_HA_USER_ID, "") ?: "",
            haAuthError = prefs.getString(KEY_HA_AUTH_ERROR, "") ?: "",
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
            alertPollMinutes = prefs.getInt(KEY_ALERT_POLL_MINUTES, 60),
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
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_HA_BASE_URL = "ha_base_url"
        private const val KEY_HA_CLIENT_ID = "ha_client_id"
        private const val KEY_HA_ACCESS_TOKEN = "ha_access_token"
        private const val KEY_HA_REFRESH_TOKEN = "ha_refresh_token"
        private const val KEY_HA_TOKEN_EXPIRY = "ha_token_expiry"
        private const val KEY_HA_USER_NAME = "ha_user_name"
        private const val KEY_HA_USER_ID = "ha_user_id"
        private const val KEY_HA_AUTH_STATE = "ha_auth_state"
        private const val KEY_HA_CODE_VERIFIER = "ha_code_verifier"
        private const val KEY_HA_AUTH_ERROR = "ha_auth_error"
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
        private const val KEY_ALERT_POLL_MINUTES = "alert_poll_minutes"
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
            KEY_PROFILE_NAME,
            KEY_HA_BASE_URL,
            KEY_HA_CLIENT_ID,
            KEY_HA_ACCESS_TOKEN,
            KEY_HA_REFRESH_TOKEN,
            KEY_HA_TOKEN_EXPIRY,
            KEY_HA_USER_NAME,
            KEY_HA_USER_ID,
            KEY_HA_AUTH_ERROR,
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
            KEY_ALERT_POLL_MINUTES,
            KEY_CAR_OBD_NAME,
            KEY_CAR_OBD_ADDRESS,
            KEY_CAR_MAKE,
            KEY_CAR_MODEL,
            KEY_CAR_YEAR,
            KEY_CAR_TRIM,
        )

        const val DEFAULT_SERVER_URL = "https://pumpkin.rosshome.co.uk"
        const val DEFAULT_HA_BASE_URL = "https://ha.rosshome.co.uk"
        const val DEFAULT_HA_CLIENT_ID = "https://pumpkin.rosshome.co.uk"
    }
}
