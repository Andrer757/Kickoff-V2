package ${configs.packageName}.sdk.core

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal API for the SDK and should not be used outside the SDK module."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSdkApi
