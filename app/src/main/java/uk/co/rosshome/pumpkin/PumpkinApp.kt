package uk.co.rosshome.pumpkin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

private enum class Screen(val label: String) {
    HOME("Home"),
    PTT("Push"),
    SETTINGS("Settings"),
    DEBUG("Debug"),
}

@Composable
fun PumpkinApp(
    settingsViewModel: SettingsViewModel,
    ingestViewModel: IngestViewModel,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.HOME) }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Screen.values().forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            label = { Text(item.label) },
                            icon = {},
                        )
                    }
                }
            },
        ) { padding ->
            when (screen) {
                Screen.HOME -> HomeScreen(settings = settings, padding = padding)
                Screen.PTT -> PushToTalkScreen(
                    ingestViewModel = ingestViewModel,
                    padding = padding,
                )
                Screen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    padding = padding,
                )
                Screen.DEBUG -> DebugScreen(
                    ingestViewModel = ingestViewModel,
                    padding = padding,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(settings: SettingsState, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Pumpkin", style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Server", style = MaterialTheme.typography.titleSmall)
                Text(text = settings.serverUrl)
                Text(text = "API Key", style = MaterialTheme.typography.titleSmall)
                Text(text = maskKey(settings.apiKey))
                Text(text = "Location", style = MaterialTheme.typography.titleSmall)
                Text(text = if (settings.includeLocation) "enabled" else "disabled")
                Text(text = "Speak responses", style = MaterialTheme.typography.titleSmall)
                Text(text = if (settings.speakResponses) "enabled" else "disabled")
            }
        }
        Text(
            text = "Use Push to send text to /ingest. Use Settings to configure server and key.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PushToTalkScreen(ingestViewModel: IngestViewModel, padding: PaddingValues) {
    var text by remember { mutableStateOf("") }
    val logs = ingestViewModel.logs
    val context = LocalContext.current
    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    var isListening by remember { mutableStateOf(false) }
    var speechStatus by remember { mutableStateOf("") }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            speechStatus = "Microphone permission denied"
        }
    }
    val listener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speechStatus = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                speechStatus = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                return
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                return
            }

            override fun onEndOfSpeech() {
                speechStatus = "Processing..."
            }

            override fun onError(error: Int) {
                isListening = false
                speechStatus = "Speech error: $error"
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val match = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!match.isNullOrBlank()) {
                    text = match
                    speechStatus = "Ready"
                } else {
                    speechStatus = "No speech detected"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    text = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                return
            }
        }
    }
    DisposableEffect(recognizer) {
        onDispose {
            recognizer?.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Push-to-talk", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text to send") },
            minLines = 3,
        )
        Button(
            onClick = {
                ingestViewModel.sendText(text)
                text = ""
            },
            enabled = text.isNotBlank() && !ingestViewModel.isSending,
        ) {
            Text(text = if (ingestViewModel.isSending) "Sending..." else "Send")
        }
        Button(
            onClick = {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }
                if (recognizer == null) {
                    speechStatus = "Speech recognizer unavailable"
                    return@Button
                }
                if (isListening) {
                    recognizer.stopListening()
                    isListening = false
                    speechStatus = "Stopped"
                } else {
                    recognizer.setRecognitionListener(listener)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    }
                    recognizer.startListening(intent)
                    isListening = true
                    speechStatus = "Starting..."
                }
            },
            enabled = !ingestViewModel.isSending,
        ) {
            Text(text = if (isListening) "Stop listening" else "Start listening")
        }
        if (speechStatus.isNotBlank()) {
            Text(text = speechStatus, style = MaterialTheme.typography.bodySmall)
        }
        ResponseSummary(ingestViewModel = ingestViewModel)
        Text(text = "Recent responses", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(logs) { entry ->
                ResponseLogCard(entry)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: SettingsState,
    settingsViewModel: SettingsViewModel,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    var serverUrl by remember(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        settingsViewModel.updateIncludeLocation(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server URL") },
            singleLine = true,
        )
        Button(onClick = { settingsViewModel.updateServerUrl(serverUrl) }) {
            Text(text = "Save server URL")
        }
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Button(onClick = { settingsViewModel.updateApiKey(apiKey) }) {
            Text(text = "Save API key")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = "Include last known location")
                if (!hasLocationPermission && settings.includeLocation) {
                    Text(text = "Location permission required")
                }
            }
            Switch(
                checked = settings.includeLocation,
                onCheckedChange = { enabled ->
                    if (enabled && !hasLocationPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        settingsViewModel.updateIncludeLocation(enabled)
                    }
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Speak responses")
            Switch(
                checked = settings.speakResponses,
                onCheckedChange = { enabled ->
                    settingsViewModel.updateSpeakResponses(enabled)
                },
            )
        }
        if (!hasLocationPermission) {
            TextButton(
                onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            ) {
                Text(text = "Request location permission")
            }
        }
    }
}

@Composable
private fun ResponseSummary(ingestViewModel: IngestViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Last response", style = MaterialTheme.typography.titleSmall)
            Text(text = ingestViewModel.lastResponse ?: "none")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Last error", style = MaterialTheme.typography.titleSmall)
            Text(text = ingestViewModel.lastError ?: "none")
        }
    }
}

@Composable
private fun DebugScreen(ingestViewModel: IngestViewModel, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Debug", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Last response", style = MaterialTheme.typography.titleSmall)
        Text(text = ingestViewModel.lastResponse ?: "none")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Last error", style = MaterialTheme.typography.titleSmall)
        Text(text = ingestViewModel.lastError ?: "none")
    }
}

@Composable
private fun ResponseLogCard(entry: IngestLogEntry) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.timestamp,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = if (entry.success) "ok" else "error",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = entry.message)
            if (!entry.responseBody.isNullOrBlank()) {
                Text(
                    text = entry.responseBody,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun maskKey(value: String): String {
    if (value.isBlank()) {
        return "(not set)"
    }
    val suffix = value.takeLast(4)
    return "****$suffix"
}
