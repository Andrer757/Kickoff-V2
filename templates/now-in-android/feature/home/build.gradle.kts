plugins {
    id("kickoff.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "${configs.packageName}.feature.home"
}

dependencies {
    implementation(project(":core:connectivity"))
    implementation(libs.kotlinx.serialization.json)
}
