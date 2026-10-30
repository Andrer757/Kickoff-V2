plugins {
    id("kickoff.android.library")
    id("kickoff.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "${configs.packageName}.core.network"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
}
