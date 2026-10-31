package ${configs.packageName}.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewModelScope
import ${configs.packageName}.core.connectivity.NetworkMonitor
import ${configs.packageName}.core.data.UserRepository
import ${configs.packageName}.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object HomeRoute

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val users: List<User>,
        val isOnline: Boolean,
        val isDarkMode: Boolean
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.users,
        networkMonitor.isOnline,
        userRepository.isDarkMode
    ) { users, isOnline, isDarkMode ->
        HomeUiState.Success(
            users = users,
            isOnline = isOnline,
            isDarkMode = isDarkMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    fun toggleDarkMode(current: Boolean) {
        viewModelScope.launch {
            userRepository.setDarkMode(!current)
        }
    }
}

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onToggleDarkMode = viewModel::toggleDarkMode
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is HomeUiState.Success -> {
                    Text(
                        text = "Welcome to ${configs.projectName}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isOnline) "Status: Online" else "Status: Offline",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (uiState.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Users in database: ${uiState.users.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onToggleDarkMode(uiState.isDarkMode) }) {
                        Text(text = if (uiState.isDarkMode) "Switch to Light Theme" else "Switch to Dark Theme")
                    }
                }
            }
        }
    }
}
