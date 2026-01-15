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
    val homeassistant: HomeassistantSummary? = null,
    val homeassistant_last_event: HomeassistantLastEvent? = null,
    val home_state: HomeStateSummary? = null,
    val network_discovery: NetworkDiscoverySnapshot? = null,
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
data class HomeassistantSummary(
    val people_home: List<String> = emptyList(),
    val people: List<HomeassistantPerson> = emptyList(),
    val zones: List<HomeassistantZone> = emptyList(),
    val calendars: List<HomeassistantCalendar> = emptyList(),
    val upcoming_events: List<HomeassistantCalendarEvent> = emptyList(),
    val calendar_error: String? = null,
)

@Serializable
data class HomeassistantPerson(
    val entity_id: String,
    val name: String? = null,
    val state: String? = null,
)

@Serializable
data class HomeassistantZone(
    val entity_id: String,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Double? = null,
    val passive: Boolean? = null,
    val icon: String? = null,
)

@Serializable
data class HomeassistantCalendar(
    val entity_id: String,
    val name: String? = null,
)

@Serializable
data class HomeassistantCalendarEvent(
    val calendar: String? = null,
    val entity_id: String? = null,
    val summary: String? = null,
    val start: JsonElement? = null,
    val end: JsonElement? = null,
    val location: String? = null,
)

@Serializable
data class HomeassistantLastEvent(
    val event_type: String? = null,
    val origin: String? = null,
    val time_fired: String? = null,
    val payload: HomeassistantEventPayload? = null,
)

@Serializable
data class HomeassistantEventPayload(
    val entity_id: String? = null,
    val state: String? = null,
    val attributes: JsonElement? = null,
)

@Serializable
data class HomeStateSummary(
    val people_home: List<String> = emptyList(),
    val doors_open: List<String> = emptyList(),
    val windows_open: List<String> = emptyList(),
    val motion_active: List<String> = emptyList(),
    val lights_on: List<String> = emptyList(),
)

@Serializable
data class NetworkDiscoverySnapshot(
    val local_ip: String? = null,
    val subnet: String? = null,
    val device_count: Int = 0,
    val devices: List<NetworkDevice> = emptyList(),
    val ssdp: List<NetworkSsdpEntry> = emptyList(),
)

@Serializable
data class NetworkDevice(
    val ip: String? = null,
    val mac: String? = null,
    val device: String? = null,
    val open_ports: List<Int> = emptyList(),
    val services: List<NetworkService> = emptyList(),
    val hints: List<String> = emptyList(),
)

@Serializable
data class NetworkService(
    val type: String? = null,
    val port: Int? = null,
    val status: String? = null,
    val server: String? = null,
    val title: String? = null,
    val banner: String? = null,
)

@Serializable
data class NetworkSsdpEntry(
    val ip: String? = null,
    @SerialName("st") val service_type: String? = null,
    val usn: String? = null,
    val server: String? = null,
    val location: String? = null,
    @SerialName("cache-control") val cache_control: String? = null,
    @SerialName("_raw") val raw: String? = null,
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

@Serializable
data class ErrorsResponse(
    val count: Int,
    val errors: List<ErrorReport> = emptyList(),
)

@Serializable
data class ErrorReport(
    val id: Int,
    val ts: String,
    val payload: JsonElement? = null,
    val severity: String? = null,
)

@Serializable
data class NotificationItem(
    val id: Int,
    val ts: String? = null,
    val message: String? = null,
    val concerns: List<String> = emptyList(),
    val anomalies: List<String> = emptyList(),
    val report_url: String? = null,
)

@Serializable
data class NotificationsResponse(
    val count: Int,
    val notifications: List<NotificationItem> = emptyList(),
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
    val profileName: String,
    val haBaseUrl: String,
    val haClientId: String,
    val haAccessToken: String,
    val haRefreshToken: String,
    val haTokenExpiry: Long,
    val haUserName: String,
    val haUserId: String,
    val haAuthError: String,
    val includeLocation: Boolean,
    val speakResponses: Boolean,
    val ttsVoiceName: String,
    val quietHours: String,
    val quietHoursDays: String,
    val notificationStyle: String,
    val assistantEnabled: Boolean,
    val assistantIncludeNotifications: Boolean,
    val assistantIncludeTriggers: Boolean,
    val assistantStartOnBoot: Boolean,
    val assistantAccessibilityEnabled: Boolean,
    val carTelemetryEnabled: Boolean,
    val carTelemetrySampleSeconds: Int,
    val carTelemetrySyncMinutes: Int,
    val alertPollMinutes: Int,
    val carObdDeviceName: String,
    val carObdDeviceAddress: String,
    val carMake: String,
    val carModel: String,
    val carYear: String,
    val carTrim: String,
)

@Serializable
data class CarTelemetryRecord(
    val ts: String,
    val device_id: String,
    val adapter_name: String? = null,
    val adapter_address: String? = null,
    val profile: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: String? = null,
    val trim: String? = null,
    val location: LocationPayload? = null,
    val readings: Map<String, Double> = emptyMap(),
)
