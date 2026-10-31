package ${configs.packageName}.sdk.core

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

public interface SdkClient {
    public val status: StateFlow<SdkStatus>
    public val config: SdkConfig

    public suspend fun initialize(): Result<Unit>
    public fun shutdown(): Unit

    public companion object {
        public fun create(context: Context, config: SdkConfig): SdkClient {
            return InternalSdkClient(context.applicationContext, config)
        }
    }
}
