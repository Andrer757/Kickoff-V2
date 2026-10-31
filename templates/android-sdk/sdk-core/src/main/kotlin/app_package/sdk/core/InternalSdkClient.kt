package ${configs.packageName}.sdk.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(InternalSdkApi::class)
@InternalSdkApi
internal class InternalSdkClient(
    private val appContext: Context,
    override val config: SdkConfig
) : SdkClient {

    private val _status = MutableStateFlow(SdkStatus.UNINITIALIZED)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    override suspend fun initialize(): Result<Unit> {
        return try {
            _status.value = SdkStatus.INITIALIZING
            _status.value = SdkStatus.READY
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = SdkStatus.ERROR
            Result.failure(e)
        }
    }

    override fun shutdown() {
        _status.value = SdkStatus.UNINITIALIZED
    }
}
