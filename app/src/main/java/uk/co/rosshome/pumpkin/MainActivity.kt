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
                    SummaryClient(),
                    AskClient(),
                ),
            )
            val proposalsViewModel: ProposalsViewModel = viewModel(
                factory = ProposalsViewModelFactory(
                    settingsRepository,
                    ProposalClient(),
                ),
            )
            val improvementsViewModel: ProposalsViewModel = viewModel(
                key = "improvements",
                factory = ProposalsViewModelFactory(
                    settingsRepository,
                    ProposalClient(),
                ),
            )
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(
                    settingsRepository,
                    SummaryClient(),
                    ErrorsClient(),
                ),
            )
            val updateViewModel: UpdateViewModel = viewModel(
                factory = UpdateViewModelFactory(UpdateClient()),
            )

            PumpkinApp(
                settingsViewModel = settingsViewModel,
                ingestViewModel = ingestViewModel,
                homeViewModel = homeViewModel,
                updateViewModel = updateViewModel,
                proposalsViewModel = proposalsViewModel,
                improvementsViewModel = improvementsViewModel,
            )
        }
    }
}
