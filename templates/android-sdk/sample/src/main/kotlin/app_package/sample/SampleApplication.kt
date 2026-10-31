package ${configs.packageName}.sample

import android.app.Application
import ${configs.packageName}.sdk.core.SdkClient
import ${configs.packageName}.sdk.core.SdkConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SampleApplication : Application() {

    lateinit var sdkClient: SdkClient
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val config = SdkConfig.Builder(apiKey = "sample-api-key")
            .setEnvironment("sandbox")
            .setDebugLoggingEnabled(true)
            .build()

        sdkClient = SdkClient.create(this, config)

        applicationScope.launch {
            sdkClient.initialize()
        }
    }
}
