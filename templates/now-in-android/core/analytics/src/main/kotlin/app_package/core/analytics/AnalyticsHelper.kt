package ${configs.packageName}.core.analytics

import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

data class AnalyticsEvent(
    val type: String,
    val properties: Map<String, Any?> = emptyMap()
)

interface AnalyticsHelper {
    fun logEvent(event: AnalyticsEvent)
    fun setUserProperty(name: String, value: String)
}

@Singleton
class LoggingAnalyticsHelper @Inject constructor() : AnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) {
        Log.d("Analytics", "Event: ${event.type}, Props: ${event.properties}")
    }

    override fun setUserProperty(name: String, value: String) {
        Log.d("Analytics", "Set user property: $name = $value")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindsAnalyticsHelper(
        analyticsHelper: LoggingAnalyticsHelper
    ): AnalyticsHelper
}
