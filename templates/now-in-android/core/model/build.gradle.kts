plugins {
    id("kickoff.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "${configs.packageName}.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
