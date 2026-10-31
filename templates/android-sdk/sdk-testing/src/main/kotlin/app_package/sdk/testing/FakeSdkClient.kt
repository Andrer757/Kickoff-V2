package ${configs.packageName}.sdk.testing

import ${configs.packageName}.sdk.core.SdkClient
import ${configs.packageName}.sdk.core.SdkConfig
import ${configs.packageName}.sdk.core.SdkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class FakeSdkClient(
    override val config: SdkConfig = SdkConfig.Builder("test-api-key").build()
) : SdkClient {

    private val _status: MutableStateFlow<SdkStatus> = MutableStateFlow(SdkStatus.READY)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    public var shouldFailInitialize: Boolean = false

    public fun setStatus(status: SdkStatus): Unit {
        _status.value = status
    }

    override suspend fun initialize(): Result<Unit> {
        return if (shouldFailInitialize) {
            _status.value = SdkStatus.ERROR
            Result.failure(IllegalStateException("Simulated initialization failure"))
        } else {
            _status.value = SdkStatus.READY
            Result.success(Unit)
        }
    }

    override fun shutdown(): Unit {
        _status.value = SdkStatus.UNINITIALIZED
    }
}
