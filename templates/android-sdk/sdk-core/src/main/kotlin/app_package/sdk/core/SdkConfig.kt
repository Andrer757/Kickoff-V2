package ${configs.packageName}.sdk.core

public class SdkConfig private constructor(
    public val apiKey: String,
    public val environment: String,
    public val debugLoggingEnabled: Boolean
) {
    public class Builder(private val apiKey: String) {
        private var environment: String = "production"
        private var debugLoggingEnabled: Boolean = false

        public fun setEnvironment(environment: String): Builder = apply {
            this.environment = environment
        }

        public fun setDebugLoggingEnabled(enabled: Boolean): Builder = apply {
            this.debugLoggingEnabled = enabled
        }

        public fun build(): SdkConfig = SdkConfig(
            apiKey = apiKey,
            environment = environment,
            debugLoggingEnabled = debugLoggingEnabled
        )
    }
}
