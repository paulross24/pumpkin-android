package uk.co.rosshome.pumpkin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import uk.co.rosshome.pumpkin.BuildConfig

private enum class Screen(val label: String) {
    HOME("Home"),
    PTT("Push"),
    PROPOSALS("Proposals"),
    IMPROVEMENTS("Improvements"),
    SETTINGS("Settings"),
    DEBUG("Debug"),
}

@Composable
fun PumpkinApp(
    settingsViewModel: SettingsViewModel,
    ingestViewModel: IngestViewModel,
    homeViewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    proposalsViewModel: ProposalsViewModel,
    improvementsViewModel: ProposalsViewModel,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            CrashUploader(context).uploadIfPresent()
        }
        updateViewModel.check()
    }
    LaunchedEffect(settings.assistantEnabled, hasNotificationPermission) {
        AssistantServiceController.stop(context)
    }

    PumpkinTheme {
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
                Screen.HOME -> HomeScreen(
                    settings = settings,
                    homeViewModel = homeViewModel,
                    updateViewModel = updateViewModel,
                    padding = padding,
                )
                Screen.PTT -> PushToTalkScreen(
                    ingestViewModel = ingestViewModel,
                    padding = padding,
                )
                Screen.PROPOSALS -> ProposalsScreen(
                    proposalsViewModel = proposalsViewModel,
                    padding = padding,
                )
                Screen.IMPROVEMENTS -> ImprovementsScreen(
                    improvementsViewModel = improvementsViewModel,
                    padding = padding,
                )
                Screen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    ingestViewModel = ingestViewModel,
                    padding = padding,
                )
                Screen.DEBUG -> DebugScreen(
                    ingestViewModel = ingestViewModel,
                    homeViewModel = homeViewModel,
                    padding = padding,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    settings: SettingsState,
    homeViewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val summary = homeViewModel.summary
    val errors = homeViewModel.errors
    val lastError = homeViewModel.lastError
    val isLoading = homeViewModel.isLoading
    val update = updateViewModel.latest
    val updateError = updateViewModel.lastError
    val isChecking = updateViewModel.isChecking
    val updateAvailable = updateViewModel.updateAvailable
    LaunchedEffect(Unit) {
        homeViewModel.refresh()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Pumpkin", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { homeViewModel.refresh() }, enabled = !isLoading) {
                Text(text = if (isLoading) "Refreshing..." else "Refresh")
            }
        }
        UpdateCard(
            update = update,
            isChecking = isChecking,
            error = updateError,
            updateAvailable = updateAvailable,
            onCheck = { updateViewModel.check() },
            onOpen = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        )
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
                Text(text = "OpenAI key", style = MaterialTheme.typography.titleSmall)
                Text(text = if (settings.openAiKey.isBlank()) "not set" else "set")
            }
        }
        if (lastError != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Last error", style = MaterialTheme.typography.titleSmall)
                    Text(text = lastError)
                }
            }
        }
        HomeSummaryCard(summary = summary)
        CalendarCard(summary = summary)
        InventoryCard(summary = summary)
        LogCard(errors = errors, summary = summary)
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
private fun ProposalsScreen(proposalsViewModel: ProposalsViewModel, padding: PaddingValues) {
    var filter by remember { mutableStateOf("pending") }
    val state by proposalsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(filter) {
        proposalsViewModel.refresh(filter, 50)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Proposals", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filter = "pending" }) {
                Text(text = "Pending")
            }
            Button(onClick = { filter = "" }) {
                Text(text = "All")
            }
            Button(onClick = { proposalsViewModel.refresh(filter, 50) }) {
                Text(text = "Refresh")
            }
        }
        if (state.isLoading) {
            Text(text = "Loading...", style = MaterialTheme.typography.bodySmall)
        }
        if (!state.error.isNullOrBlank()) {
            Text(text = "Error: ${state.error}", style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.proposals) { proposal ->
                ProposalCard(
                    proposal = proposal,
                    onApprove = { proposalsViewModel.approve(proposal.id) },
                    onReject = { proposalsViewModel.reject(proposal.id) },
                )
            }
        }
    }
}

@Composable
private fun ImprovementsScreen(improvementsViewModel: ProposalsViewModel, padding: PaddingValues) {
    val state by improvementsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        improvementsViewModel.refresh(status = "approved", limit = 50)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Improvements", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { improvementsViewModel.refresh(status = "approved", limit = 50) }) {
                Text(text = "Refresh")
            }
        }
        if (state.isLoading) {
            Text(text = "Loading...", style = MaterialTheme.typography.bodySmall)
        }
        if (!state.error.isNullOrBlank()) {
            Text(text = "Error: ${state.error}", style = MaterialTheme.typography.bodySmall)
        }
        if (state.proposals.isEmpty() && !state.isLoading) {
            Text(text = "No improvements recorded yet.", style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.proposals) { proposal ->
                ImprovementCard(proposal = proposal)
            }
        }
    }
}

private fun extractSteps(details: JsonElement?): List<String> {
    return try {
        val stepsElement = details?.jsonObject?.get("steps") as? JsonArray
        stepsElement?.mapNotNull { it.jsonPrimitive.contentOrNull }?.filter { it.isNotBlank() }
            ?: emptyList()
    } catch (exc: Exception) {
        emptyList()
    }
}

@Composable
private fun ImprovementCard(proposal: Proposal) {
    val rationale = try {
        proposal.details?.jsonObject?.get("rationale")?.jsonPrimitive?.content
    } catch (exc: Exception) {
        null
    }
    val actionType = try {
        proposal.details?.jsonObject?.get("action_type")?.jsonPrimitive?.content
    } catch (exc: Exception) {
        null
    }
    val actionParams = try {
        proposal.details?.jsonObject?.get("action_params")?.jsonObject?.toString()
    } catch (exc: Exception) {
        null
    }
    val steps = extractSteps(proposal.details)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = proposal.summary, style = MaterialTheme.typography.titleSmall)
            Text(text = "Status: ${proposal.status} | Risk: ${proposal.risk}")
            Text(text = "Kind: ${proposal.kind}")
            if (!rationale.isNullOrBlank()) {
                Text(text = "Rationale: $rationale", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Expected: ${proposal.expected_outcome}", style = MaterialTheme.typography.bodySmall)
            if (!proposal.ai_context_excerpt.isNullOrBlank()) {
                Text(
                    text = proposal.ai_context_excerpt,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!actionType.isNullOrBlank()) {
                Text(text = "Action: $actionType", style = MaterialTheme.typography.bodySmall)
            }
            if (!actionParams.isNullOrBlank()) {
                Text(text = "Params: $actionParams", style = MaterialTheme.typography.bodySmall)
            }
            if (steps.isNotEmpty()) {
                Text(text = "Actuation steps:", style = MaterialTheme.typography.bodySmall)
                steps.forEach { step ->
                    Text(text = "- $step", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    text = "Actuation steps: (not provided yet)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: Proposal,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = proposal.summary, style = MaterialTheme.typography.titleSmall)
            Text(text = "Status: ${proposal.status} | Risk: ${proposal.risk}")
            Text(text = "Kind: ${proposal.kind}")
            val rationale = try {
                proposal.details?.jsonObject?.get("rationale")?.jsonPrimitive?.content
            } catch (exc: Exception) {
                null
            }
            if (!rationale.isNullOrBlank()) {
                Text(text = "Rationale: $rationale", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Expected: ${proposal.expected_outcome}", style = MaterialTheme.typography.bodySmall)
            if (!proposal.ai_context_excerpt.isNullOrBlank()) {
                Text(
                    text = proposal.ai_context_excerpt,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onApprove) {
                    Text(text = "Approve")
                }
                Button(onClick = onReject) {
                    Text(text = "Reject")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    settings: SettingsState,
    settingsViewModel: SettingsViewModel,
    ingestViewModel: IngestViewModel,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val notificationListenerEnabled = AssistantStatus.isNotificationListenerEnabled(context)
    val accessibilityEnabled = AssistantStatus.isAccessibilityServiceEnabled(context)

    var serverUrl by remember(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var openAiKey by remember(settings.openAiKey) { mutableStateOf(settings.openAiKey) }
    var quietHours by remember(settings.quietHours) { mutableStateOf(settings.quietHours) }
    var quietDays by remember(settings.quietHoursDays) { mutableStateOf(settings.quietHoursDays) }
    var notificationStyle by remember(settings.notificationStyle) { mutableStateOf(settings.notificationStyle) }
    var voiceMenuOpen by remember { mutableStateOf(false) }
    var daysMenuOpen by remember { mutableStateOf(false) }
    var styleMenuOpen by remember { mutableStateOf(false) }
    val voiceOptions = ingestViewModel.availableVoices
    val scope = rememberCoroutineScope()
    var llmStatus by remember { mutableStateOf<String?>(null) }
    var mediaStatus by remember { mutableStateOf<String?>(null) }
    val mediaHelper = remember { MediaControlHelper(context.applicationContext) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        settingsViewModel.updateIncludeLocation(granted)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            settingsViewModel.updateAssistantEnabled(true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "Settings", style = MaterialTheme.typography.headlineSmall) }
        item {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Server URL") },
                singleLine = true,
            )
        }
        item { Button(onClick = { settingsViewModel.updateServerUrl(serverUrl) }) { Text(text = "Save server URL") } }
        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item { Button(onClick = { settingsViewModel.updateApiKey(apiKey) }) { Text(text = "Save API key") } }
        item {
            OutlinedTextField(
                value = openAiKey,
                onValueChange = { openAiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OpenAI API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            Button(
                onClick = {
                    settingsViewModel.updateOpenAiKey(openAiKey)
                    scope.launch {
                        val status = withContext(Dispatchers.IO) {
                            LlmConfigClient().pushConfig(settings)
                        }.fold(
                            onSuccess = { "sent to server" },
                            onFailure = { "failed to send" },
                        )
                        llmStatus = status
                    }
                },
            ) {
                Text(text = "Save OpenAI key")
            }
        }
        item {
            if (!llmStatus.isNullOrBlank()) {
                Text(text = "LLM config: $llmStatus", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
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
        }
        item {
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
        }
        item {
            if (settings.speakResponses) {
                ExposedDropdownMenuBox(
                    expanded = voiceMenuOpen,
                    onExpandedChange = { voiceMenuOpen = !voiceMenuOpen },
                ) {
                    OutlinedTextField(
                        value = if (settings.ttsVoiceName.isBlank()) "System default" else settings.ttsVoiceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Voice") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceMenuOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = voiceMenuOpen,
                        onDismissRequest = { voiceMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("System default") },
                            onClick = {
                                settingsViewModel.updateTtsVoiceName("")
                                voiceMenuOpen = false
                            },
                        )
                        voiceOptions.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice) },
                                onClick = {
                                    settingsViewModel.updateTtsVoiceName(voice)
                                    voiceMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Assistant", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(text = "Enable assistant")
                            if (!hasNotificationPermission) {
                                Text(text = "Notification permission required")
                            }
                        }
                        Switch(
                            checked = settings.assistantEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    settingsViewModel.updateAssistantEnabled(enabled)
                                }
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(text = "Ingest notifications")
                            if (!notificationListenerEnabled) {
                                Text(text = "Enable notification access in system settings")
                            }
                        }
                        Switch(
                            checked = settings.assistantIncludeNotifications,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateAssistantIncludeNotifications(enabled)
                            },
                        )
                    }
                    if (!notificationListenerEnabled) {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                )
                            },
                        ) {
                            Text(text = "Open notification access settings")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Ingest system triggers")
                        Switch(
                            checked = settings.assistantIncludeTriggers,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateAssistantIncludeTriggers(enabled)
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Start on boot")
                        Switch(
                            checked = settings.assistantStartOnBoot,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateAssistantStartOnBoot(enabled)
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(text = "Accessibility bridge")
                            if (!accessibilityEnabled) {
                                Text(text = "Enable accessibility in system settings")
                            }
                        }
                        Switch(
                            checked = settings.assistantAccessibilityEnabled,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateAssistantAccessibilityEnabled(enabled)
                            },
                        )
                    }
                    if (!accessibilityEnabled) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                        ) {
                            Text(text = "Open accessibility settings")
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Music", style = MaterialTheme.typography.titleSmall)
                    Text(text = "Amazon Music")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                mediaStatus = if (AmazonMusicLauncher.open(context)) {
                                    "Opened Amazon Music"
                                } else {
                                    "Amazon Music not installed"
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Open app")
                        }
                        Button(
                            onClick = { mediaStatus = mediaHelper.playPause() },
                            enabled = notificationListenerEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Play/Pause")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { mediaStatus = mediaHelper.previous() },
                            enabled = notificationListenerEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Prev")
                        }
                        Button(
                            onClick = { mediaStatus = mediaHelper.next() },
                            enabled = notificationListenerEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Next")
                        }
                    }
                    if (!notificationListenerEnabled) {
                        Text(text = "Enable notification access to control playback.")
                    }
                    if (!mediaStatus.isNullOrBlank()) {
                        Text(text = mediaStatus ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Preferences", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = quietHours,
                        onValueChange = { quietHours = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quiet hours (HH:MM-HH:MM)") },
                        singleLine = true,
                    )
                    ExposedDropdownMenuBox(
                        expanded = daysMenuOpen,
                        onExpandedChange = { daysMenuOpen = !daysMenuOpen },
                    ) {
                        OutlinedTextField(
                            value = quietDays,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quiet hours days") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = daysMenuOpen) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = daysMenuOpen,
                            onDismissRequest = { daysMenuOpen = false },
                        ) {
                            listOf("weekdays", "weekends", "daily").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        quietDays = option
                                        daysMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            settingsViewModel.updateQuietHours(quietHours)
                            settingsViewModel.updateQuietHoursDays(quietDays)
                            ingestViewModel.sendPreferenceCommand(
                                "set quiet hours $quietHours $quietDays",
                            )
                        },
                    ) {
                        Text(text = "Apply quiet hours")
                    }
                    ExposedDropdownMenuBox(
                        expanded = styleMenuOpen,
                        onExpandedChange = { styleMenuOpen = !styleMenuOpen },
                    ) {
                        OutlinedTextField(
                            value = notificationStyle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Notification style") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleMenuOpen) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = styleMenuOpen,
                            onDismissRequest = { styleMenuOpen = false },
                        ) {
                            listOf("brief", "normal", "detailed").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        notificationStyle = option
                                        styleMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            settingsViewModel.updateNotificationStyle(notificationStyle)
                            ingestViewModel.sendPreferenceCommand(
                                "set notification style $notificationStyle",
                            )
                        },
                    ) {
                        Text(text = "Apply notification style")
                    }
                    if (!ingestViewModel.preferenceStatus.isNullOrBlank()) {
                        Text(
                            text = ingestViewModel.preferenceStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            if (!hasLocationPermission) {
                TextButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                ) {
                    Text(text = "Request location permission")
                }
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
            Text(text = ingestViewModel.lastHumanResponse ?: "none")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Last error", style = MaterialTheme.typography.titleSmall)
            Text(text = ingestViewModel.lastError ?: "none")
        }
    }
}

@Composable
private fun DebugScreen(
    ingestViewModel: IngestViewModel,
    homeViewModel: HomeViewModel,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val uploader = remember { CrashUploader(context) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val summary = homeViewModel.summary
    val errors = homeViewModel.errors
    LaunchedEffect(Unit) {
        homeViewModel.refresh()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Logs", style = MaterialTheme.typography.headlineSmall)
        if (summary?.homeassistant_last_event != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Last HA event", style = MaterialTheme.typography.titleSmall)
                    Text(text = formatHaEvent(summary.homeassistant_last_event))
                }
            }
        }
        if (errors.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Recent errors", style = MaterialTheme.typography.titleSmall)
                    errors.take(5).forEach { err ->
                        Text(text = "${err.ts} • ${err.severity ?: "warn"}")
                    }
                }
            }
        }
        Text(text = "Last response", style = MaterialTheme.typography.titleSmall)
        Text(text = ingestViewModel.lastResponse ?: "none")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Last error", style = MaterialTheme.typography.titleSmall)
        Text(text = ingestViewModel.lastError ?: "none")
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    val status = withContext(Dispatchers.IO) {
                        uploader.uploadIfPresent()
                    }
                    uploadStatus = status
                }
            },
        ) {
            Text(text = "Upload crash report")
        }
        Text(
            text = "Crash report: " + if (uploader.hasReport()) "stored" else "none",
            style = MaterialTheme.typography.bodySmall,
        )
        if (!uploadStatus.isNullOrBlank()) {
            Text(text = "Upload status: $uploadStatus", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HomeSummaryCard(summary: SummaryResponse?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "House status", style = MaterialTheme.typography.titleSmall)
            if (summary == null) {
                Text(text = "No summary yet.")
                return@Column
            }
            val homeState = summary.home_state
            val peopleHome = homeState?.people_home ?: summary.homeassistant?.people_home ?: emptyList()
            Text(text = if (peopleHome.isEmpty()) "No one is marked as home." else "Home: ${peopleHome.joinToString()}")
            if (!homeState?.doors_open.isNullOrEmpty()) {
                Text(text = "Doors open: ${homeState?.doors_open?.joinToString()}")
            }
            if (!homeState?.windows_open.isNullOrEmpty()) {
                Text(text = "Windows open: ${homeState?.windows_open?.joinToString()}")
            }
            if (!homeState?.motion_active.isNullOrEmpty()) {
                Text(text = "Motion: ${homeState?.motion_active?.joinToString()}")
            }
            if (!homeState?.lights_on.isNullOrEmpty()) {
                Text(text = "Lights on: ${homeState?.lights_on?.joinToString()}")
            }
            if (summary.issues.isNotEmpty()) {
                Text(text = "Issues: ${summary.issues.joinToString { it.message }}")
            }
        }
    }
}

@Composable
private fun CalendarCard(summary: SummaryResponse?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Calendar", style = MaterialTheme.typography.titleSmall)
            val events = summary?.homeassistant?.upcoming_events ?: emptyList()
            if (events.isEmpty()) {
                Text(text = "No upcoming events.")
            } else {
                events.take(3).forEach { event ->
                    val whenText = formatCalendarTime(event.start)
                    Text(text = "${event.summary ?: "Untitled"} • $whenText")
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(summary: SummaryResponse?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Inventory", style = MaterialTheme.typography.titleSmall)
            val homeState = summary?.home_state
            Text(text = "Doors open: " + (homeState?.doors_open?.joinToString() ?: "none"))
            Text(text = "Windows open: " + (homeState?.windows_open?.joinToString() ?: "none"))
            Text(text = "Lights on: " + (homeState?.lights_on?.joinToString() ?: "none"))
        }
    }
}

@Composable
private fun LogCard(errors: List<ErrorReport>, summary: SummaryResponse?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Live log", style = MaterialTheme.typography.titleSmall)
            val lastEvent = summary?.homeassistant_last_event
            if (lastEvent != null) {
                Text(text = "Last HA: ${formatHaEvent(lastEvent)}")
            } else {
                Text(text = "Last HA: none")
            }
            if (errors.isNotEmpty()) {
                Text(text = "Recent errors:")
                errors.take(3).forEach { err ->
                    Text(text = "${err.ts} • ${err.severity ?: "warn"}")
                }
            } else {
                Text(text = "No recent errors.")
            }
        }
    }
}

@Composable
private fun UpdateCard(
    update: ReleaseInfo?,
    isChecking: Boolean,
    error: String?,
    updateAvailable: Boolean,
    onCheck: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Updates", style = MaterialTheme.typography.titleSmall)
            when {
                isChecking -> Text(text = "Checking for updates...")
                error != null -> Text(text = "Update check failed: $error")
                update == null -> Text(text = "No update info yet.")
                else -> {
                    Text(text = "Installed: ${BuildConfig.VERSION_NAME}")
                    Text(text = "Latest: ${update.tag}")
                    if (updateAvailable) {
                        val url = update.apkUrl ?: update.htmlUrl
                        Button(onClick = { onOpen(url) }) {
                            Text(text = "Open download")
                        }
                    } else {
                        Text(text = "You're up to date.")
                    }
                }
            }
            TextButton(onClick = onCheck) {
                Text(text = "Check again")
            }
        }
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

private fun formatHaEvent(event: HomeassistantLastEvent): String {
    val payload = event.payload
    return if (payload?.entity_id != null && payload.state != null) {
        "${payload.entity_id} -> ${payload.state}"
    } else {
        event.event_type ?: "event"
    }
}

private fun formatCalendarTime(element: JsonElement?): String {
    if (element == null) {
        return "unscheduled"
    }
    return try {
        val obj = element.jsonObject
        val dt = obj["dateTime"]?.jsonPrimitive?.content
        val date = obj["date"]?.jsonPrimitive?.content
        dt ?: date ?: element.jsonPrimitive.content
    } catch (exc: Exception) {
        "unscheduled"
    }
}

private fun maskKey(value: String): String {
    if (value.isBlank()) {
        return "(not set)"
    }
    val suffix = value.takeLast(4)
    return "****$suffix"
}
