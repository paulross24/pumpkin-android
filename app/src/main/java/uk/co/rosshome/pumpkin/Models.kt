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

@Serializable
data class ProposalDecisionResponse(
    val status: String,
    val id: Int,
    val decision: String,
)

@Serializable
data class ProposalDecisionRequest(
    val id: Int,
    val reason: String? = null,
    val actor: String? = null,
)

@Serializable
data class SummaryResponse(
    val status: String,
    val heartbeat: SummaryEvent? = null,
    val system_snapshot: SystemSnapshot? = null,
    val issues: List<SummaryIssue> = emptyList(),
    val proposals: List<SummaryProposal> = emptyList(),
    val proposal_count: Int = 0,
)

@Serializable
data class AskResponse(
    val status: String,
    val reply: String,
)

@Serializable
data class AskRequest(
    val text: String,
    val source: String,
    val device: String,
    val ts: String,
    val location: LocationPayload? = null,
)

@Serializable
data class SummaryEvent(
    val id: Int,
    val ts: String,
    val source: String,
    val type: String,
    val payload: JsonElement? = null,
    val severity: String,
)

@Serializable
data class SummaryIssue(
    val kind: String,
    val message: String,
)

@Serializable
data class SummaryProposal(
    val id: Int,
    val kind: String,
    val summary: String,
    val risk: Double,
    val status: String,
    val expected_outcome: String,
    val ts_created: String,
)

@Serializable
data class SystemSnapshot(
    val loadavg: LoadAvg? = null,
    val disk: DiskUsage? = null,
    val meminfo_kb: MemInfo? = null,
)

@Serializable
data class LoadAvg(
    @SerialName("1m") val one: Double? = null,
    @SerialName("5m") val five: Double? = null,
    @SerialName("15m") val fifteen: Double? = null,
)

@Serializable
data class DiskUsage(
    val path: String? = null,
    val total_bytes: Long? = null,
    val used_bytes: Long? = null,
    val free_bytes: Long? = null,
    val used_percent: Double? = null,
)

@Serializable
data class MemInfo(
    val MemTotal: Long? = null,
    val MemAvailable: Long? = null,
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
    val openAiKey: String,
    val includeLocation: Boolean,
    val speakResponses: Boolean,
    val ttsVoiceName: String,
)
