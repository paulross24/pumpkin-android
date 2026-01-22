package uk.co.rosshome.pumpkin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.Charset
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CarTelemetryManager(
    context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
    private val ingestClient: IngestClient = IngestClient(),
    private val locationProvider: LocationProvider = LocationProvider(context),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = CarTelemetryStore(appContext)
    private val json = Json { encodeDefaults = true }
    private var sampleJob: Job? = null
    private var syncJob: Job? = null
    private var connection: ObdConnection? = null
    private var adapterName: String? = null
    private var adapterAddress: String? = null
    private val deviceId: String =
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    fun start() {
        if (sampleJob != null) return
        sampleJob = scope.launch { sampleLoop() }
        syncJob = scope.launch { syncLoop() }
    }

    fun stop() {
        sampleJob?.cancel()
        syncJob?.cancel()
        sampleJob = null
        syncJob = null
        connection?.close()
        connection = null
    }

    private suspend fun sampleLoop() {
        while (scope.isActive) {
            val settings = settingsRepository.readSettings()
            if (!settings.carTelemetryEnabled) {
                delay(10_000)
                continue
            }
            if (!hasBluetoothPermission()) {
                delay(15_000)
                continue
            }
            ensureConnected(settings)
            val active = connection
            if (active == null) {
                delay(15_000)
                continue
            }
            val sample = active.readTelemetrySample()
            if (sample.readings.isNotEmpty() || sample.dtcsActive.isNotEmpty() || sample.dtcsPending.isNotEmpty()) {
                val location = if (settings.includeLocation && hasLocationPermission()) {
                    val last = locationProvider.lastKnownLocation()
                    if (last != null) {
                        LocationPayload(
                            lat = last.latitude,
                            lon = last.longitude,
                            accuracy = last.accuracy.toDouble(),
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
                val record = CarTelemetryRecord(
                    ts = Instant.now().toString(),
                    device_id = deviceId,
                    adapter_name = adapterName,
                    adapter_address = adapterAddress,
                    profile = settings.profileName.ifBlank { null },
                    make = settings.carMake.ifBlank { null },
                    model = settings.carModel.ifBlank { null },
                    year = settings.carYear.ifBlank { null },
                    trim = settings.carTrim.ifBlank { null },
                    location = location,
                    readings = sample.readings,
                    dtcs_active = sample.dtcsActive,
                    dtcs_pending = sample.dtcsPending,
                    readiness = sample.readiness,
                )
                store.append(record)
            }
            val delaySeconds = max(5, settings.carTelemetrySampleSeconds)
            delay(delaySeconds * 1000L)
        }
    }

    private suspend fun syncLoop() {
        while (scope.isActive) {
            val settings = settingsRepository.readSettings()
            if (!settings.carTelemetryEnabled) {
                delay(60_000)
                continue
            }
            uploadPending(settings)
            val delayMinutes = max(5, settings.carTelemetrySyncMinutes)
            delay(delayMinutes * 60_000L)
        }
    }

    private suspend fun uploadPending(settings: SettingsState) {
        val batch = store.readBatch(limit = 25)
        if (batch.isEmpty()) return
        var sent = 0
        for (record in batch) {
            val text = "car.telemetry " + json.encodeToString(record)
            val result = withContext(Dispatchers.IO) {
                ingestClient.sendIngest(
                    text = text,
                    settings = settings,
                    deviceId = deviceId,
                    location = record.location,
                    source = "android-car",
                )
            }
            if (!result.success) {
                break
            }
            sent += 1
        }
        if (sent > 0) {
            store.dropBatch(sent)
        }
    }

    private suspend fun ensureConnected(settings: SettingsState) {
        val active = connection
        if (active != null && active.isOpen()) {
            return
        }
        connection?.close()
        connection = null
        adapterName = null
        adapterAddress = null

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val devices = adapter.bondedDevices.orEmpty()
        if (devices.isEmpty()) return

        val preferredAddress = settings.carObdDeviceAddress.trim()
        val preferredName = settings.carObdDeviceName.trim()
        val ordered = devices.sortedBy { it.name ?: "" }
        val candidates = ordered.filter { device ->
            if (preferredAddress.isNotBlank()) {
                device.address.equals(preferredAddress, ignoreCase = true)
            } else if (preferredName.isNotBlank()) {
                device.name?.contains(preferredName, ignoreCase = true) == true
            } else {
                val name = device.name ?: ""
                name.contains("obd", ignoreCase = true) ||
                    name.contains("elm", ignoreCase = true) ||
                    name.contains("vlink", ignoreCase = true)
            }
        }.ifEmpty { ordered }

        for (device in candidates) {
            val conn = ObdConnection(device)
            if (conn.connect()) {
                if (conn.initialize()) {
                    connection = conn
                    adapterName = device.name
                    adapterAddress = device.address
                    if (preferredAddress.isBlank()) {
                        settingsRepository.updateCarObdDeviceAddress(device.address)
                    }
                    return
                }
                conn.close()
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private data class CarTelemetrySample(
    val readings: Map<String, Double>,
    val dtcsActive: List<String>,
    val dtcsPending: List<String>,
    val readiness: CarReadiness?,
)

class CarTelemetryStore(context: Context) {
    private val file = File(context.filesDir, "car_telemetry.jsonl")
    private val json = Json { encodeDefaults = true }

    fun append(record: CarTelemetryRecord) {
        val line = json.encodeToString(record) + "\n"
        file.appendText(line)
    }

    fun readBatch(limit: Int): List<CarTelemetryRecord> {
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        if (lines.isEmpty()) return emptyList()
        val batch = lines.take(limit)
        return batch.mapNotNull { line ->
            runCatching { json.decodeFromString(CarTelemetryRecord.serializer(), line) }.getOrNull()
        }
    }

    fun dropBatch(count: Int) {
        if (!file.exists()) return
        val lines = file.readLines()
        if (lines.isEmpty()) return
        val remaining = if (count >= lines.size) emptyList() else lines.drop(count)
        if (remaining.isEmpty()) {
            file.delete()
        } else {
            file.writeText(remaining.joinToString("\n") + "\n")
        }
    }
}

class ObdConnection(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null

    fun connect(): Boolean {
        return runCatching {
            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()
            socket = sock
            input = BufferedInputStream(sock.inputStream)
            output = BufferedOutputStream(sock.outputStream)
            true
        }.getOrDefault(false)
    }

    fun isOpen(): Boolean = socket?.isConnected == true

    fun initialize(): Boolean {
        sendCommand("ATZ")
        sendCommand("ATE0")
        sendCommand("ATL0")
        sendCommand("ATH0")
        sendCommand("ATS0")
        sendCommand("ATSP0")
        val response = sendCommand("0100")
        return response.isNotBlank() && !response.contains("NO DATA", ignoreCase = true)
    }

    fun readTelemetrySample(): CarTelemetrySample {
        val readings = mutableMapOf<String, Double>()
        for (pid in PIDS) {
            val response = sendCommand(pid.command)
            val parsed = pid.parse(response)
            if (parsed != null) {
                readings[pid.key] = parsed
            }
        }
        val readiness = readReadiness()
        val dtcsActive = readDtcs("03")
        val dtcsPending = readDtcs("07")
        return CarTelemetrySample(
            readings = readings,
            dtcsActive = dtcsActive,
            dtcsPending = dtcsPending,
            readiness = readiness,
        )
    }

    fun close() {
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        input = null
        output = null
        socket = null
    }

    private fun sendCommand(command: String): String {
        val out = output ?: return ""
        return runCatching {
            out.write((command + "\r").toByteArray(Charset.defaultCharset()))
            out.flush()
            readResponse()
        }.getOrDefault("")
    }

    private fun readResponse(): String {
        val inputStream = input ?: return ""
        val buffer = StringBuilder()
        val start = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - start < RESPONSE_TIMEOUT_MS) {
            val available = inputStream.available()
            if (available > 0) {
                val byte = inputStream.read().toChar()
                if (byte == '>') break
                buffer.append(byte)
            } else {
                Thread.sleep(20)
            }
        }
        return buffer.toString()
    }

    private fun readReadiness(): CarReadiness? {
        val response = sendCommand("0101")
        val data = parsePidBytes(response, "0101")
        if (data.isEmpty()) return null
        val first = data.getOrNull(0) ?: return null
        val milOn = (first and 0x80) != 0
        val dtcCount = first and 0x7F
        val raw = data.take(4).joinToString(" ") { "%02X".format(it) }
        return CarReadiness(mil_on = milOn, dtc_count = dtcCount, raw = raw)
    }

    private fun readDtcs(mode: String): List<String> {
        val response = sendCommand(mode)
        if (response.contains("NO DATA", ignoreCase = true)) return emptyList()
        val cleaned = response.uppercase()
            .replace("SEARCHING...", "")
            .replace("NO DATA", "")
            .replace(Regex("[^0-9A-F]"), "")
        val marker = if (mode == "03") "43" else "47"
        val idx = cleaned.indexOf(marker)
        if (idx == -1) return emptyList()
        val payload = cleaned.substring(idx + marker.length)
        if (payload.length < 4) return emptyList()
        val bytes = payload.chunked(2).mapNotNull { it.toIntOrNull(16) }
        val dtcs = mutableListOf<String>()
        for (i in bytes.indices step 2) {
            val a = bytes.getOrNull(i) ?: break
            val b = bytes.getOrNull(i + 1) ?: break
            if (a == 0 && b == 0) continue
            val type = when ((a and 0xC0) shr 6) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                else -> "U"
            }
            val digit1 = (a and 0x30) shr 4
            val digit2 = a and 0x0F
            val digit3 = (b and 0xF0) shr 4
            val digit4 = b and 0x0F
            val code = "$type$digit1${digit2.toString(16).uppercase()}${digit3.toString(16).uppercase()}${digit4.toString(16).uppercase()}"
            dtcs.add(code)
        }
        return dtcs
    }

    private fun parsePidBytes(raw: String, command: String): List<Int> {
        if (raw.contains("NO DATA", ignoreCase = true)) return emptyList()
        val cleaned = raw.uppercase()
            .replace("SEARCHING...", "")
            .replace("NO DATA", "")
            .replace(Regex("[^0-9A-F]"), "")
        val pid = command.removePrefix("01").removePrefix("0").padStart(2, '0')
        val marker = "41" + pid
        val idx = cleaned.indexOf(marker)
        if (idx == -1) return emptyList()
        val payload = cleaned.substring(idx + marker.length)
        if (payload.length < 2) return emptyList()
        return payload.chunked(2).mapNotNull {
            runCatching { it.toInt(16) }.getOrNull()
        }
    }

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val RESPONSE_TIMEOUT_MS = 2000L

        private val PIDS = listOf(
            ObdPid("010C", "rpm") { bytes ->
                val a = bytes.getOrNull(0) ?: return@ObdPid null
                val b = bytes.getOrNull(1) ?: return@ObdPid null
                ((a * 256) + b) / 4.0
            },
            ObdPid("010D", "speed_kph") { bytes -> bytes.getOrNull(0)?.toDouble() },
            ObdPid("0105", "coolant_c") { bytes -> bytes.getOrNull(0)?.minus(40)?.toDouble() },
            ObdPid("010F", "intake_c") { bytes -> bytes.getOrNull(0)?.minus(40)?.toDouble() },
            ObdPid("0111", "throttle_pct") { bytes -> bytes.getOrNull(0)?.times(100.0)?.div(255.0) },
            ObdPid("0104", "engine_load_pct") { bytes -> bytes.getOrNull(0)?.times(100.0)?.div(255.0) },
            ObdPid("012F", "fuel_level_pct") { bytes -> bytes.getOrNull(0)?.times(100.0)?.div(255.0) },
            ObdPid("0110", "maf_gps") { bytes ->
                val a = bytes.getOrNull(0) ?: return@ObdPid null
                val b = bytes.getOrNull(1) ?: return@ObdPid null
                ((a * 256) + b) / 100.0
            },
            ObdPid("011F", "engine_runtime_s") { bytes ->
                val a = bytes.getOrNull(0) ?: return@ObdPid null
                val b = bytes.getOrNull(1) ?: return@ObdPid null
                ((a * 256) + b).toDouble()
            },
            ObdPid("015E", "fuel_rate_lph") { bytes ->
                val a = bytes.getOrNull(0) ?: return@ObdPid null
                val b = bytes.getOrNull(1) ?: return@ObdPid null
                ((a * 256) + b) / 20.0
            },
            ObdPid("0106", "stft_b1_pct") { bytes -> bytes.getOrNull(0)?.let { (it - 128) * 100.0 / 128.0 } },
            ObdPid("0107", "ltft_b1_pct") { bytes -> bytes.getOrNull(0)?.let { (it - 128) * 100.0 / 128.0 } },
            ObdPid("0108", "stft_b2_pct") { bytes -> bytes.getOrNull(0)?.let { (it - 128) * 100.0 / 128.0 } },
            ObdPid("0109", "ltft_b2_pct") { bytes -> bytes.getOrNull(0)?.let { (it - 128) * 100.0 / 128.0 } },
            ObdPid("010A", "fuel_pressure_kpa") { bytes -> bytes.getOrNull(0)?.times(3.0) },
            ObdPid("010B", "map_kpa") { bytes -> bytes.getOrNull(0)?.toDouble() },
            ObdPid("010E", "timing_advance_deg") { bytes -> bytes.getOrNull(0)?.let { it / 2.0 - 64.0 } },
            ObdPid("0131", "distance_since_codes_km") { bytes ->
                val a = bytes.getOrNull(0) ?: return@ObdPid null
                val b = bytes.getOrNull(1) ?: return@ObdPid null
                ((a * 256) + b).toDouble()
            },
            ObdPid("0114", "o2_b1s1_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0114", "o2_b1s1_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("0115", "o2_b1s2_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0115", "o2_b1s2_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("0116", "o2_b1s3_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0116", "o2_b1s3_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("0117", "o2_b1s4_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0117", "o2_b1s4_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("0118", "o2_b2s1_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0118", "o2_b2s1_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("0119", "o2_b2s2_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("0119", "o2_b2s2_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("011A", "o2_b2s3_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("011A", "o2_b2s3_trim_pct") { bytes -> parseO2Trim(bytes) },
            ObdPid("011B", "o2_b2s4_v") { bytes -> parseO2Voltage(bytes) },
            ObdPid("011B", "o2_b2s4_trim_pct") { bytes -> parseO2Trim(bytes) },
        )

        private fun parseO2Voltage(bytes: List<Int>): Double? {
            val a = bytes.getOrNull(0) ?: return null
            return a / 200.0
        }

        private fun parseO2Trim(bytes: List<Int>): Double? {
            val b = bytes.getOrNull(1) ?: return null
            return (b - 128) * 100.0 / 128.0
        }
    }
}

class ObdPid(
    val command: String,
    val key: String,
    private val parser: (List<Int>) -> Double?,
) {
    fun parse(raw: String): Double? {
        val data = parsePidBytes(raw, command)
        if (data.isEmpty()) return null
        return parser(data)
    }

    private fun parsePidBytes(raw: String, command: String): List<Int> {
        if (raw.contains("NO DATA", ignoreCase = true)) return emptyList()
        val cleaned = raw.uppercase()
            .replace("SEARCHING...", "")
            .replace("NO DATA", "")
            .replace(Regex("[^0-9A-F]"), "")
        val pid = command.removePrefix("01").removePrefix("0").padStart(2, '0')
        val marker = "41" + pid
        val idx = cleaned.indexOf(marker)
        if (idx == -1) return emptyList()
        val payload = cleaned.substring(idx + marker.length)
        if (payload.length < 2) return emptyList()
        return payload.chunked(2).mapNotNull {
            runCatching { it.toInt(16) }.getOrNull()
        }
    }
}
