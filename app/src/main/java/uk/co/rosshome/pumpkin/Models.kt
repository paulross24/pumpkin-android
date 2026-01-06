package uk.co.rosshome.pumpkin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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

@Serializable
data class Proposal(
    val id: Int,
    val kind: String,
    val summary: String,
    val details: JsonElement? = null,
    val risk: Double,
    val expected_outcome: String,
    val status: String,
    val needs_new_capability: Boolean,
    val capability_request: String? = null,
    val ai_context_excerpt: String? = null,
    val ts_created: String,
)

@Serializable
data class ProposalsResponse(
    val count: Int,
    val proposals: List<Proposal>,
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
    val speakResponses: Boolean,
    val ttsVoiceName: String,
)
