plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.quality.verifier) apply false
}

apiValidation {
    ignoredProjects += listOf("sample", "sdk-testing")
    nonPublicMarkers += listOf(
        "${configs.packageName}.sdk.core.InternalSdkApi"
    )
}

allprojects {
    apply(plugin = "eu.andrer757.quality-verifier")
}
