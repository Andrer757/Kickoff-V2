package ${configs.packageName}.core.testing

import ${configs.packageName}.core.connectivity.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline.asStateFlow()

    fun setConnected(isConnected: Boolean) {
        _isOnline.value = isConnected
    }
}
