package ${configs.packageName}.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ${configs.packageName}.core.connectivity.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object HomeRoute

@HiltViewModel
class HomeViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline.stateIn(
        scope = androidx.lifecycle.viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
}

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    HomeScreen(isOnline = isOnline)
}

@Composable
fun HomeScreen(isOnline: Boolean) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to ${configs.projectName}",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (isOnline) "Status: Online" else "Status: Offline",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
