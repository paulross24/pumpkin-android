package uk.co.rosshome.pumpkin

import android.Manifest
import android.content.pm.PackageManager
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
