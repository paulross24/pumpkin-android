package uk.co.rosshome.pumpkin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(settingsRepository),
            )
            val ingestViewModel: IngestViewModel = viewModel(
                factory = IngestViewModelFactory(
                    application,
                    settingsRepository,
                    IngestClient(),
                    LocationProvider(applicationContext),
                ),
            )
            val proposalsViewModel: ProposalsViewModel = viewModel(
                factory = ProposalsViewModelFactory(
                    settingsRepository,
                    ProposalClient(),
                ),
            )

            PumpkinApp(
                settingsViewModel = settingsViewModel,
                ingestViewModel = ingestViewModel,
                proposalsViewModel = proposalsViewModel,
            )
        }
    }
}
