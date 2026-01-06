package uk.co.rosshome.pumpkin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationPayload(
    val lat: Double,
    val lon: Double,
    @SerialName("acc") val accuracy: Double? = null,
)

@Serializable
data class IngestRequest(
    val text: String,
    val source: String,
    val device: String,
    val ts: String,
    val location: LocationPayload? = null,
)

@Serializable
data class IngestResponse(
    val status: String? = null,
    val received: Map<String, String>? = null,
)

data class IngestLogEntry(
    val timestamp: String,
    val success: Boolean,
    val message: String,
    val responseBody: String? = null,
)

data class SettingsState(
    val serverUrl: String,
    val apiKey: String,
    val includeLocation: Boolean,
)
